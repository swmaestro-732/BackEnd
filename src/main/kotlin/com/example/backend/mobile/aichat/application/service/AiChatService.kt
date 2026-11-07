package com.example.backend.mobile.aichat.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.AiChatErrorCode
import com.example.backend.mobile.aichat.application.port.inbound.AiChatUseCase
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatInputType
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatMessageCommand
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatQuickReply
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatStep
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatTurnResult
import com.example.backend.mobile.aichat.application.port.outbound.AiChatCourseCriteria
import com.example.backend.mobile.aichat.application.port.outbound.AiChatCoursePort
import com.example.backend.mobile.aichat.application.port.outbound.AiChatLocationPort
import com.example.backend.mobile.aichat.application.port.outbound.AiChatPlace
import com.example.backend.mobile.aichat.application.port.outbound.AiChatPlacePort
import com.example.backend.mobile.aichat.application.port.outbound.AiChatSessionPort
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatCourse
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.PlaceSlot
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.RegionSlot
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 커미 AI 대화 엔진(규칙 기반). 단계마다 답변을 슬롯으로 바꾸고, 셋이 모이면 추천을 실행한다.
 * 해석 실패는 에러가 아니라 같은 단계를 안내 문구와 함께 다시 내리고, 현재 단계에 없는 칩·막힌 입력은 400(4001)이다.
 * 추천은 조건을 하나씩 풀며 찾는다: 장소 제거 → 테마를 가산으로 → 지역 확대(동 → 시군구 → 시도) → 전체 인기.
 */
@Service
class AiChatService(
    private val sessionPort: AiChatSessionPort,
    private val coursePort: AiChatCoursePort,
    private val placePort: AiChatPlacePort,
    private val locationPort: AiChatLocationPort,
    private val areaQueryUseCase: AreaQueryUseCase,
) : AiChatUseCase {
    private val log = KotlinLogging.logger {}

    override fun sendMessage(command: AiChatMessageCommand): AiChatTurnResult {
        val session = loadOrCreate(command)
        val (next, turn) =
            when (session.step) {
                AiChatStep.REGION -> onRegion(session, command)
                AiChatStep.THEME -> onTheme(session, command)
                AiChatStep.PLACE -> onPlace(session, command)
                AiChatStep.PLACE_CONFIRM -> onPlaceConfirm(session, command)
                AiChatStep.RESULT -> onResult(session, command)
            }
        sessionPort.save(next)
        return turn
    }

    private fun loadOrCreate(command: AiChatMessageCommand): AiChatSession {
        val sessionId =
            command.sessionId?.takeIf { it.isNotBlank() }
                ?: return AiChatSession(
                    id = UUID.randomUUID().toString(),
                    userId = command.userId,
                    step = AiChatStep.REGION,
                )
        val session = sessionPort.find(sessionId) ?: throw BusinessException(AiChatErrorCode.AI_CHAT_SESSION_NOT_FOUND)
        // 다른 사용자의 세션은 없는 것으로 취급한다(존재 여부를 드러내지 않음).
        if (session.userId != command.userId) throw BusinessException(AiChatErrorCode.AI_CHAT_SESSION_NOT_FOUND)
        return session
    }

    // ----- REGION -----

    private fun onRegion(
        session: AiChatSession,
        command: AiChatMessageCommand,
    ): Pair<AiChatSession, AiChatTurnResult> =
        when (command.type) {
            AiChatInputType.CHIP -> {
                requireChip(command.value == NEAR_ME, command.value)
                val location = command.location ?: return retryRegion(session, MSG_REGION_NO_LOCATION)
                val areaCode =
                    locationPort.findAreaCodeAt(location) ?: return retryRegion(session, MSG_REGION_LOCATION_FAILED)
                val label = areaQueryUseCase.findAreaByCode(areaCode)?.shortName ?: LABEL_NEAR_ME
                askTheme(session.copy(region = RegionSlot(label = label, prefixes = listOf(areaCode))))
            }

            AiChatInputType.TEXT -> {
                val text = command.text!!.trim()
                val prefixes = resolveRegion(text)
                if (prefixes.isEmpty()) {
                    retryRegion(session, MSG_REGION_NOT_FOUND)
                } else {
                    askTheme(
                        session.copy(
                            region = RegionSlot(label = AiChatRegionAliases.normalize(text), prefixes = prefixes),
                        ),
                    )
                }
            }
        }

    /** 직접 입력 → 법정동코드 prefix 목록. 별칭 표로 검색어를 바꾼 뒤 지역 마스터 이름 검색에 맡긴다. */
    private fun resolveRegion(text: String): List<String> {
        val normalized = AiChatRegionAliases.normalize(text)
        if (normalized.length < MIN_REGION_LENGTH) return emptyList()
        return AiChatRegionAliases
            .searchKeywords(normalized)
            .flatMap { areaQueryUseCase.resolveSearchPrefixes(it) }
            .distinct()
    }

    private fun retryRegion(
        session: AiChatSession,
        message: String,
    ): Pair<AiChatSession, AiChatTurnResult> =
        session.copy(step = AiChatStep.REGION) to
            turn(session, AiChatStep.REGION, message, quickReplies = emptyList(), placeholder = PLACEHOLDER_REGION)

    // ----- THEME -----

    private fun askTheme(session: AiChatSession): Pair<AiChatSession, AiChatTurnResult> =
        session.copy(step = AiChatStep.THEME) to
            turn(session, AiChatStep.THEME, MSG_THEME_ASK, AiChatThemeDictionary.CHIPS, PLACEHOLDER_THEME)

    private fun onTheme(
        session: AiChatSession,
        command: AiChatMessageCommand,
    ): Pair<AiChatSession, AiChatTurnResult> {
        val theme =
            when (command.type) {
                AiChatInputType.CHIP -> {
                    val value = command.value!!
                    requireChip(value == AiChatThemeDictionary.ANY || value in AiChatThemeDictionary.CATEGORIES, value)
                    AiChatThemeDictionary.slotOf(value.takeUnless { it == AiChatThemeDictionary.ANY })
                }

                AiChatInputType.TEXT -> {
                    val slot = AiChatThemeDictionary.classify(command.text!!)
                    // 직접 입력 비율·사전 미적중 비율은 2차 LLM 우선순위 판단용 지표다.
                    log.info {
                        "ai-chat theme text: category=${slot.category} likeThemes=${slot.likeThemes} keyword=${slot.keyword != null}"
                    }
                    slot
                }
            }
        return askPlace(session.copy(theme = theme), MSG_PLACE_ASK)
    }

    // ----- PLACE -----

    private fun askPlace(
        session: AiChatSession,
        message: String,
    ): Pair<AiChatSession, AiChatTurnResult> =
        session.copy(step = AiChatStep.PLACE, placeCandidates = emptyMap()) to
            turn(
                session,
                AiChatStep.PLACE,
                message,
                listOf(AiChatQuickReply(LABEL_NO_PLACE, NO_PLACE)),
                PLACEHOLDER_PLACE,
            )

    private fun onPlace(
        session: AiChatSession,
        command: AiChatMessageCommand,
    ): Pair<AiChatSession, AiChatTurnResult> =
        when (command.type) {
            AiChatInputType.CHIP -> {
                requireChip(command.value == NO_PLACE, command.value)
                recommend(session.copy(place = null))
            }

            AiChatInputType.TEXT -> {
                val name = command.text!!.trim()
                val candidates = placePort.searchByName(name, PLACE_CANDIDATE_LIMIT)
                val exact = candidates.filter { it.name.replace(" ", "") == name.replace(" ", "") }
                when {
                    candidates.isEmpty() -> askPlace(session, MSG_PLACE_NOT_FOUND)
                    candidates.size == 1 -> recommend(session.copy(place = candidates.single().toSlot()))
                    exact.size == 1 -> recommend(session.copy(place = exact.single().toSlot()))
                    else -> confirmPlace(session, candidates)
                }
            }
        }

    private fun confirmPlace(
        session: AiChatSession,
        candidates: List<AiChatPlace>,
    ): Pair<AiChatSession, AiChatTurnResult> {
        val byValue = candidates.associate { "$PLACE_VALUE_PREFIX${it.id}" to it.toSlot() }
        val chips =
            candidates.map { place ->
                AiChatQuickReply(
                    label = listOfNotNull(place.name, place.categoryLabel, place.dongName).joinToString(" · "),
                    value = "$PLACE_VALUE_PREFIX${place.id}",
                )
            } + AiChatQuickReply(LABEL_PLACE_NOT_LISTED, PLACE_NOT_LISTED)
        return session.copy(step = AiChatStep.PLACE_CONFIRM, placeCandidates = byValue) to
            turn(session, AiChatStep.PLACE_CONFIRM, MSG_PLACE_CONFIRM, chips, placeholder = null, inputEnabled = false)
    }

    private fun onPlaceConfirm(
        session: AiChatSession,
        command: AiChatMessageCommand,
    ): Pair<AiChatSession, AiChatTurnResult> {
        require(command.type == AiChatInputType.CHIP) { MSG_INPUT_DISABLED }
        val value = command.value!!
        if (value == PLACE_NOT_LISTED) return askPlace(session, MSG_PLACE_RETRY)
        val place = session.placeCandidates[value]
        requireChip(place != null, value)
        return recommend(session.copy(place = place, placeCandidates = emptyMap()))
    }

    // ----- RESULT -----

    private fun onResult(
        session: AiChatSession,
        command: AiChatMessageCommand,
    ): Pair<AiChatSession, AiChatTurnResult> {
        require(command.type == AiChatInputType.CHIP) { MSG_INPUT_DISABLED }
        requireChip(command.value == RETRY, command.value)
        return recommend(session)
    }

    private fun recommend(session: AiChatSession): Pair<AiChatSession, AiChatTurnResult> {
        val (courses, relaxation) = findCourses(session)
        if (courses.isEmpty()) {
            return session.copy(step = AiChatStep.RESULT) to
                turn(
                    session,
                    AiChatStep.RESULT,
                    MSG_NO_MORE,
                    listOf(AiChatQuickReply(LABEL_RESTART, RESTART)),
                    placeholder = null,
                    inputEnabled = false,
                )
        }
        val message =
            when (relaxation) {
                Relaxation.NONE -> MSG_RESULT
                Relaxation.PLACE_DROPPED -> MSG_RESULT_PLACE_DROPPED
                Relaxation.THEME_RELAXED -> MSG_RESULT_THEME_RELAXED
                Relaxation.REGION_WIDENED -> MSG_RESULT_REGION_WIDENED.format(session.region?.label ?: LABEL_NEAR_ME)
                Relaxation.POPULAR -> MSG_RESULT_POPULAR
            }
        return session.copy(
            step = AiChatStep.RESULT,
            shownCourseIds = session.shownCourseIds + courses.map { it.id },
        ) to
            turn(
                session,
                AiChatStep.RESULT,
                message,
                listOf(AiChatQuickReply(LABEL_RETRY, RETRY), AiChatQuickReply(LABEL_RESTART, RESTART)),
                placeholder = null,
                inputEnabled = false,
                courses = courses,
            )
    }

    /** 조건을 좁은 것부터 넓은 것까지 순서대로 시도해 처음 결과가 나오는 조건을 쓴다. */
    private fun findCourses(session: AiChatSession): Pair<List<AiChatCourse>, Relaxation> {
        val regionLevels = regionLevels(session.region?.prefixes.orEmpty())
        val theme = session.theme
        val narrowest = regionLevels.first()
        val attempts =
            buildList {
                add(Attempt(narrowest, session.place?.id, theme?.category, Relaxation.NONE))
                if (session.place != null) add(Attempt(narrowest, null, theme?.category, Relaxation.PLACE_DROPPED))
                if (theme?.category != null) add(Attempt(narrowest, null, null, Relaxation.THEME_RELAXED))
                regionLevels.drop(1).forEach { add(Attempt(it, null, null, Relaxation.REGION_WIDENED)) }
                add(Attempt(emptyList(), null, null, Relaxation.POPULAR))
            }.distinctBy { Triple(it.regionPrefixes, it.placeId, it.category) }
        for (attempt in attempts) {
            val courses =
                coursePort.recommend(
                    AiChatCourseCriteria(
                        regionPrefixes = attempt.regionPrefixes,
                        category = attempt.category,
                        likeThemes = theme?.likeThemes.orEmpty(),
                        keyword = theme?.keyword,
                        placeId = attempt.placeId,
                        excludeIds = session.shownCourseIds,
                        size = COURSE_COUNT,
                    ),
                )
            if (courses.isNotEmpty()) return courses to attempt.relaxation
        }
        return emptyList<AiChatCourse>() to Relaxation.POPULAR
    }

    /** prefix 목록을 좁은 순서로 넓힌다: 입력 그대로 → 시군구(5자리) → 시도(2자리). 이미 그 길이면 건너뛴다. */
    private fun regionLevels(prefixes: List<String>): List<List<String>> {
        if (prefixes.isEmpty()) return listOf(emptyList())
        val levels = mutableListOf(prefixes)
        for (length in listOf(SIGUNGU_PREFIX_LENGTH, SIDO_PREFIX_LENGTH)) {
            if (prefixes.none { it.length > length }) continue
            val wider = prefixes.map { it.take(length) }.distinct()
            if (wider != levels.last()) levels.add(wider)
        }
        return levels
    }

    private data class Attempt(
        val regionPrefixes: List<String>,
        val placeId: Long?,
        val category: String?,
        val relaxation: Relaxation,
    )

    private enum class Relaxation { NONE, PLACE_DROPPED, THEME_RELAXED, REGION_WIDENED, POPULAR }

    // ----- helpers -----

    private fun turn(
        session: AiChatSession,
        step: AiChatStep,
        message: String,
        quickReplies: List<AiChatQuickReply>,
        placeholder: String?,
        inputEnabled: Boolean = true,
        courses: List<AiChatCourse> = emptyList(),
    ) = AiChatTurnResult(
        sessionId = session.id,
        step = step,
        messages = listOf(message),
        quickReplies = quickReplies,
        inputEnabled = inputEnabled,
        inputPlaceholder = placeholder,
        courses = courses,
    )

    private fun requireChip(
        condition: Boolean,
        value: String?,
    ) = require(condition) { "현재 단계에서 고를 수 없는 칩입니다: $value" }

    private fun AiChatPlace.toSlot() = PlaceSlot(id = id, name = name)

    companion object {
        const val NEAR_ME = "NEAR_ME"
        const val NO_PLACE = "NO_PLACE"
        const val PLACE_NOT_LISTED = "PLACE_NOT_LISTED"
        const val PLACE_VALUE_PREFIX = "place:"
        const val RETRY = "RETRY"
        const val RESTART = "RESTART"

        const val COURSE_COUNT = 3
        const val PLACE_CANDIDATE_LIMIT = 5
        private const val MIN_REGION_LENGTH = 2
        private const val SIGUNGU_PREFIX_LENGTH = 5
        private const val SIDO_PREFIX_LENGTH = 2

        private const val LABEL_NEAR_ME = "내 근처"
        private const val LABEL_NO_PLACE = "없어요, 바로 추천받기"
        private const val LABEL_PLACE_NOT_LISTED = "이 중에 없어요"
        private const val LABEL_RETRY = "다시 추천받기"
        private const val LABEL_RESTART = "처음부터"

        private const val PLACEHOLDER_REGION = "지역을 입력해도 돼요"
        private const val PLACEHOLDER_THEME = "원하는 분위기를 적어도 돼요"
        private const val PLACEHOLDER_PLACE = "장소 이름을 입력해 주세요"

        private const val MSG_REGION_NO_LOCATION = "위치 권한이 없어서 근처를 찾을 수 없어요. 아래에서 지역을 골라주시거나 입력해 주세요."
        private const val MSG_REGION_LOCATION_FAILED = "지금 위치를 확인하지 못했어요. 아래에서 지역을 골라주시거나 입력해 주세요."
        private const val MSG_REGION_NOT_FOUND = "죄송해요, 그 지역을 찾지 못했어요. 아래에서 골라주시거나 다시 입력해 주세요."
        private const val MSG_THEME_ASK = "좋아요! 원하는 코스 테마가 있나요?"
        private const val MSG_PLACE_ASK = "특별히 가고 싶은 장소가 있나요? 없으면 바로 추천해 드릴게요."
        private const val MSG_PLACE_NOT_FOUND = "그 장소를 찾지 못했어요. 다른 이름으로 입력하거나 바로 추천받을 수도 있어요."
        private const val MSG_PLACE_RETRY = "그럼 다른 이름으로 다시 입력해 주시거나, 장소 없이 바로 추천받아도 좋아요."
        private const val MSG_PLACE_CONFIRM = "어느 곳인가요?"
        private const val MSG_INPUT_DISABLED = "이 단계에서는 칩으로만 답할 수 있습니다."
        private const val MSG_RESULT = "이런 코스는 어때요? 마음에 들면 저장해 두세요."
        private const val MSG_RESULT_PLACE_DROPPED = "그 장소가 들어간 코스는 아직 없어요. 대신 근처 코스를 골라봤어요."
        private const val MSG_RESULT_THEME_RELAXED = "딱 맞는 테마의 코스가 없어서 비슷한 분위기의 코스를 골라봤어요."
        private const val MSG_RESULT_REGION_WIDENED = "%s 안에는 코스가 적어서 주변까지 넓혀 봤어요."
        private const val MSG_RESULT_POPULAR = "조건에 맞는 코스가 아직 없어서 인기 코스를 골라봤어요."
        private const val MSG_NO_MORE = "더 보여드릴 코스가 없어요. 조건을 바꿔서 다시 찾아볼까요?"
    }
}
