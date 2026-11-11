package com.example.backend.mobile.aichat.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.bootstrap.config.AiChatLlmProperties
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
import com.example.backend.mobile.aichat.application.port.outbound.AiChatSlotExtractionPort
import com.example.backend.mobile.aichat.application.port.outbound.ExtractedSlots
import com.example.backend.mobile.aichat.application.port.outbound.SlotExtractionRequest
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatCourse
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.PlaceSlot
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.RegionSlot
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.ThemeSlot
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 커미 AI 대화 엔진. 단계마다 답변을 슬롯으로 바꾸고, 셋이 모이면 추천을 실행한다.
 * 칩은 그대로 값이 되고, 직접 입력은 LLM([AiChatSlotExtractionPort])이 슬롯 JSON 으로 바꾼 뒤 마스터 데이터로 검증한다 —
 * 테마는 LLM 을 먼저 쓰고(사전은 LLM 이 없을 때의 폴백), 지역·장소는 규칙(지역 마스터·장소 검색)이 실패했을 때만 LLM 으로 문장을 정리해 다시 찾는다.
 * 한 문장에 여러 슬롯이 들어 있으면 뒤 질문을 건너뛴다. LLM 이 없거나 실패하면 규칙 기반으로 진행한다.
 * 해석 실패는 에러가 아니라 같은 단계를 안내 문구와 함께 다시 내리고, 현재 단계에 없는 칩·막힌 입력은 400(4001)이다.
 * 추천은 조건을 하나씩 풀며 찾는다: 장소 제거 → 테마를 가산으로 → 지역 확대(동 → 시군구 → 시도) → 전체 인기.
 * 좁은 조건의 결과가 3개 미만이면 다음 단계에서 부족한 수만 더 가져와 채운다(좁은 결과가 앞에 온다).
 */
@Service
class AiChatService(
    private val sessionPort: AiChatSessionPort,
    private val coursePort: AiChatCoursePort,
    private val placePort: AiChatPlacePort,
    private val locationPort: AiChatLocationPort,
    private val slotExtractionPort: AiChatSlotExtractionPort,
    private val areaQueryUseCase: AreaQueryUseCase,
    private val llmProperties: AiChatLlmProperties,
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
                if (prefixes.isNotEmpty()) {
                    return askTheme(
                        session.copy(
                            region = RegionSlot(label = AiChatRegionAliases.normalize(text), prefixes = prefixes),
                        ),
                    )
                }
                // 지역 마스터로 못 풀면 LLM 이 문장에서 지역 이름(과 함께 말한 테마·장소)을 꺼내 다시 찾는다.
                val (withCalls, slots) = extractSlots(session, text)
                when {
                    slots == null -> {
                        retryRegion(withCalls, MSG_REGION_NOT_FOUND)
                    }

                    slots.offTopic -> {
                        retryRegion(withCalls, MSG_OFF_TOPIC_REGION)
                    }

                    slots.nearMe && slots.regionText == null -> {
                        retryRegion(withCalls, MSG_REGION_USE_NEAR_ME)
                    }

                    else -> {
                        val resolved = slots.regionText?.let { resolveRegion(it) }.orEmpty()
                        if (resolved.isEmpty()) {
                            retryRegion(withCalls, MSG_REGION_NOT_FOUND)
                        } else {
                            continueAfterRegion(
                                withCalls.copy(region = RegionSlot(label = slots.regionText!!, prefixes = resolved)),
                                slots,
                            )
                        }
                    }
                }
            }
        }

    /** 지역이 채워진 뒤 — 같은 문장에 테마·장소까지 있었으면 그 질문을 건너뛴다. */
    private fun continueAfterRegion(
        session: AiChatSession,
        slots: ExtractedSlots,
    ): Pair<AiChatSession, AiChatTurnResult> {
        val theme = themeFromSlots(slots) ?: return askTheme(session)
        return continueAfterTheme(session.copy(theme = theme), slots)
    }

    /**
     * LLM 슬롯 추출. 세션당 상한을 넘으면 호출하지 않는다(null). 호출 횟수를 올린 세션을 함께 돌려준다.
     */
    private fun extractSlots(
        session: AiChatSession,
        text: String,
    ): Pair<AiChatSession, ExtractedSlots?> {
        if (session.llmCalls >= llmProperties.maxCallsPerSession) return session to null
        val slots =
            slotExtractionPort.extract(
                SlotExtractionRequest(
                    step = session.step,
                    text = text,
                    regionLabel = session.region?.label,
                    themeCategory = session.theme?.category,
                ),
            )
        return session.copy(llmCalls = session.llmCalls + 1) to slots
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
        when (command.type) {
            AiChatInputType.CHIP -> {
                val value = command.value!!
                requireChip(value == AiChatThemeDictionary.ANY || value in AiChatThemeDictionary.CATEGORIES, value)
                val theme = AiChatThemeDictionary.slotOf(value.takeUnless { it == AiChatThemeDictionary.ANY })
                return askPlace(session.copy(theme = theme), MSG_PLACE_ASK)
            }

            AiChatInputType.TEXT -> {
                val text = command.text!!.trim()
                // 테마는 문장을 12종으로 분류하는 일이라 LLM 을 먼저 쓴다. 없거나 실패하면 동의어 사전.
                val (withCalls, slots) = extractSlots(session, text)
                if (slots == null) {
                    val theme = AiChatThemeDictionary.classify(text)
                    log.info {
                        "ai-chat theme by dictionary: category=${theme.category} likeThemes=${theme.likeThemes}"
                    }
                    return askPlace(withCalls.copy(theme = theme), MSG_PLACE_ASK)
                }
                if (slots.offTopic) {
                    return withCalls to
                        turn(
                            withCalls,
                            AiChatStep.THEME,
                            MSG_OFF_TOPIC_THEME,
                            AiChatThemeDictionary.CHIPS,
                            PLACEHOLDER_THEME,
                        )
                }
                val theme = themeFromSlots(slots) ?: AiChatThemeDictionary.slotOf(null)
                return continueAfterTheme(withCalls.copy(theme = theme), slots)
            }
        }
    }

    /**
     * LLM 결과 → 테마 슬롯. 12종에 없는 코드는 버린다(검증). 테마도 키워드도 없으면 null(문장에 테마가 없음).
     * 하나면 필터, 여러 개면 가산, 키워드만 있으면 키워드 가산.
     */
    private fun themeFromSlots(slots: ExtractedSlots): ThemeSlot? {
        val themes = slots.themes.filter { it in AiChatThemeDictionary.CATEGORIES }
        val keyword = slots.themeKeywords.joinToString(" ").ifBlank { null }
        return when {
            themes.size == 1 -> ThemeSlot(category = themes.single(), keyword = keyword, likeThemes = themes)
            themes.size > 1 -> ThemeSlot(category = null, keyword = keyword, likeThemes = themes)
            keyword != null -> ThemeSlot(category = null, keyword = keyword, likeThemes = emptyList())
            else -> null
        }
    }

    /** 테마가 채워진 뒤 — 같은 문장에서 장소 없음·장소 이름까지 나왔으면 장소 질문을 건너뛴다. */
    private fun continueAfterTheme(
        session: AiChatSession,
        slots: ExtractedSlots,
    ): Pair<AiChatSession, AiChatTurnResult> =
        when {
            slots.noPlace -> {
                recommend(session.copy(step = AiChatStep.PLACE, place = null))
            }

            slots.placeName != null -> {
                searchPlace(
                    session.copy(step = AiChatStep.PLACE),
                    slots.placeName,
                    allowLlm = false,
                )
            }

            else -> {
                askPlace(session, MSG_PLACE_ASK)
            }
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
                searchPlace(session, command.text!!.trim(), allowLlm = true)
            }
        }

    /**
     * 장소 이름 검색 → 확정·되묻기·재질문. 입력 그대로 검색해 없으면([allowLlm] 일 때) LLM 이 문장에서 가게 이름만 꺼내 한 번 더 찾는다.
     * "없어요"를 문장으로 말하면 바로 추천한다.
     */
    private fun searchPlace(
        session: AiChatSession,
        name: String,
        allowLlm: Boolean,
    ): Pair<AiChatSession, AiChatTurnResult> {
        val candidates = placePort.searchByName(name, PLACE_CANDIDATE_LIMIT)
        if (candidates.isEmpty() && allowLlm) {
            val (withCalls, slots) = extractSlots(session, name)
            return when {
                slots == null -> {
                    askPlace(withCalls, MSG_PLACE_NOT_FOUND)
                }

                slots.noPlace -> {
                    recommend(withCalls.copy(place = null))
                }

                slots.offTopic -> {
                    askPlace(withCalls, MSG_OFF_TOPIC_PLACE)
                }

                slots.placeName != null && slots.placeName != name -> {
                    searchPlace(
                        withCalls,
                        slots.placeName,
                        allowLlm = false,
                    )
                }

                else -> {
                    askPlace(withCalls, MSG_PLACE_NOT_FOUND)
                }
            }
        }
        val exact = candidates.filter { it.name.replace(" ", "") == name.replace(" ", "") }
        return when {
            candidates.isEmpty() -> askPlace(session, MSG_PLACE_NOT_FOUND)
            candidates.size == 1 -> recommend(session.copy(place = candidates.single().toSlot()))
            exact.size == 1 -> recommend(session.copy(place = exact.single().toSlot()))
            else -> confirmPlace(session, candidates)
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
        val found = findCourses(session)
        val courses = found.courses
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
        val message = resultMessage(session, found)
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

    /** 가장 많이 푼 단계 기준 안내 문구. 좁은 조건 결과와 섞였으면([Found.mixed]) "적어서 … 도 함께" 로 말한다. */
    private fun resultMessage(
        session: AiChatSession,
        found: Found,
    ): String {
        val regionLabel = session.region?.label ?: LABEL_NEAR_ME
        return when (found.relaxation) {
            Relaxation.NONE -> if (session.shownCourseIds.isEmpty()) MSG_RESULT else MSG_RESULT_MORE
            Relaxation.PLACE_DROPPED -> if (found.mixed) MSG_RESULT_PLACE_DROPPED_MIXED else MSG_RESULT_PLACE_DROPPED
            Relaxation.THEME_RELAXED -> if (found.mixed) MSG_RESULT_THEME_RELAXED_MIXED else MSG_RESULT_THEME_RELAXED
            Relaxation.REGION_WIDENED -> MSG_RESULT_REGION_WIDENED.format(regionLabel)
            Relaxation.POPULAR -> if (found.mixed) MSG_RESULT_POPULAR_MIXED else MSG_RESULT_POPULAR
        }
    }

    /**
     * 조건을 좁은 것부터 넓은 것까지 순서대로 시도해 [COURSE_COUNT] 개를 채운다.
     * 단계마다 부족한 수만 요청하고 이미 담은 코스는 제외한다. [Found.relaxation] 은 결과를 낸 가장 넓은 단계다.
     */
    private fun findCourses(session: AiChatSession): Found {
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
        val collected = mutableListOf<AiChatCourse>()
        var relaxation = Relaxation.NONE
        var contributingAttempts = 0
        for (attempt in attempts) {
            val remaining = COURSE_COUNT - collected.size
            if (remaining <= 0) break
            val courses =
                coursePort.recommend(
                    AiChatCourseCriteria(
                        regionPrefixes = attempt.regionPrefixes,
                        category = attempt.category,
                        likeThemes = theme?.likeThemes.orEmpty(),
                        keyword = theme?.keyword,
                        placeId = attempt.placeId,
                        excludeIds = session.shownCourseIds + collected.map { it.id },
                        size = remaining,
                    ),
                )
            if (courses.isEmpty()) continue
            collected += courses.take(remaining)
            relaxation = attempt.relaxation
            contributingAttempts++
        }
        return Found(collected, relaxation, mixed = contributingAttempts > 1)
    }

    private data class Found(
        val courses: List<AiChatCourse>,
        val relaxation: Relaxation,
        val mixed: Boolean,
    )

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
        private const val LABEL_RETRY = "더 추천받기"
        private const val LABEL_RESTART = "처음부터"

        private const val PLACEHOLDER_REGION = "지역을 입력해도 돼요"
        private const val PLACEHOLDER_THEME = "원하는 분위기를 적어도 돼요"
        private const val PLACEHOLDER_PLACE = "장소 이름을 입력해 주세요"

        private const val MSG_REGION_NO_LOCATION = "위치 권한이 없어서 근처를 찾을 수 없어요. 아래에서 지역을 골라주시거나 입력해 주세요."
        private const val MSG_REGION_LOCATION_FAILED = "지금 위치를 확인하지 못했어요. 아래에서 지역을 골라주시거나 입력해 주세요."
        private const val MSG_REGION_NOT_FOUND = "죄송해요, 그 지역을 찾지 못했어요. 아래에서 골라주시거나 다시 입력해 주세요."
        private const val MSG_REGION_USE_NEAR_ME = "근처 코스를 찾으려면 아래 \"내 근처\"를 눌러 주세요. 지역 이름을 입력해도 돼요."
        private const val MSG_OFF_TOPIC_REGION = "저는 코스 찾기를 도와드려요. 먼저 어느 지역으로 가볼지 알려주세요."
        private const val MSG_OFF_TOPIC_THEME = "저는 코스 찾기를 도와드려요. 원하는 코스 테마를 골라주시거나 분위기를 적어 주세요."
        private const val MSG_OFF_TOPIC_PLACE = "저는 코스 찾기를 도와드려요. 가고 싶은 장소 이름을 적어 주시거나 바로 추천받아도 좋아요."
        private const val MSG_THEME_ASK = "좋아요! 원하는 코스 테마가 있나요?"
        private const val MSG_PLACE_ASK = "특별히 가고 싶은 장소가 있나요? 없으면 바로 추천해 드릴게요."
        private const val MSG_PLACE_NOT_FOUND = "그 장소를 찾지 못했어요. 다른 이름으로 입력하거나 바로 추천받을 수도 있어요."
        private const val MSG_PLACE_RETRY = "그럼 다른 이름으로 다시 입력해 주시거나, 장소 없이 바로 추천받아도 좋아요."
        private const val MSG_PLACE_CONFIRM = "어느 곳인가요?"
        private const val MSG_INPUT_DISABLED = "이 단계에서는 칩으로만 답할 수 있습니다."
        private const val MSG_RESULT = "이런 코스는 어때요? 마음에 들면 저장해 두세요."
        private const val MSG_RESULT_MORE = "이런 코스도 있어요. 아직 안 보신 코스만 골랐어요."
        private const val MSG_RESULT_PLACE_DROPPED = "그 장소가 들어간 코스는 아직 없어요. 대신 근처 코스를 골라봤어요."
        private const val MSG_RESULT_PLACE_DROPPED_MIXED = "그 장소가 들어간 코스가 적어서 근처 코스도 함께 골라봤어요."
        private const val MSG_RESULT_THEME_RELAXED = "딱 맞는 테마의 코스가 없어서 비슷한 분위기의 코스를 골라봤어요."
        private const val MSG_RESULT_THEME_RELAXED_MIXED = "딱 맞는 테마의 코스가 적어서 비슷한 분위기의 코스도 함께 골라봤어요."
        private const val MSG_RESULT_REGION_WIDENED = "%s 안에는 코스가 적어서 주변까지 넓혀 봤어요."
        private const val MSG_RESULT_POPULAR = "조건에 맞는 코스가 아직 없어서 인기 코스를 골라봤어요."
        private const val MSG_RESULT_POPULAR_MIXED = "조건에 맞는 코스가 적어서 인기 코스도 함께 골라봤어요."
        private const val MSG_NO_MORE = "더 보여드릴 코스가 없어요. 조건을 바꿔서 다시 찾아볼까요?"
    }
}
