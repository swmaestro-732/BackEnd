package com.example.backend.mobile.aichat.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.domain.model.AreaLevel
import com.example.backend.bootstrap.config.AiChatLlmProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.AiChatErrorCode
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatInputType
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatMessageCommand
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * [AiChatService] — 단계별 칩·직접 입력 해석, LLM 우선/폴백·호출 상한, 다중 슬롯 건너뛰기, 조건 완화 사다리, 더 추천받기 제외를 검증한다.
 */
class AiChatServiceTest {
    private val sessions = FakeSessionPort()
    private val courses = FakeCoursePort()
    private val places = FakePlacePort()
    private val location = FakeLocationPort()
    private val llm = FakeSlotPort()
    private val area = mock(AreaQueryUseCase::class.java)

    private fun service(maxCalls: Int = 20) =
        AiChatService(
            sessions,
            courses,
            places,
            location,
            llm,
            area,
            AiChatLlmProperties(maxCallsPerSession = maxCalls),
        )

    private val seongsu = RegionSlot(label = "성수", prefixes = listOf(SEONGSU_DONG))
    private val onion = AiChatPlace(id = 10L, name = "어니언 성수", categoryLabel = "카페·디저트", dongName = "성수동2가")
    private val onionAnguk = AiChatPlace(id = 11L, name = "어니언 안국", categoryLabel = "카페·디저트", dongName = "계동")

    private fun seed(
        step: AiChatStep,
        region: RegionSlot? = seongsu,
        theme: ThemeSlot? = null,
        place: PlaceSlot? = null,
        candidates: Map<String, PlaceSlot> = emptyMap(),
        shown: List<Long> = emptyList(),
        llmCalls: Int = 0,
        userId: Long = USER_ID,
    ): AiChatSession =
        AiChatSession(
            id = SESSION_ID,
            userId = userId,
            step = step,
            region = region,
            theme = theme,
            place = place,
            placeCandidates = candidates,
            shownCourseIds = shown,
            llmCalls = llmCalls,
        ).also { sessions.save(it) }

    private fun chip(
        value: String,
        sessionId: String? = SESSION_ID,
        location: Coordinate? = null,
    ) = AiChatMessageCommand(USER_ID, sessionId, AiChatInputType.CHIP, value, null, location)

    private fun text(
        text: String,
        sessionId: String? = SESSION_ID,
    ) = AiChatMessageCommand(USER_ID, sessionId, AiChatInputType.TEXT, null, text, null)

    private fun saved() = sessions.saved.getValue(SESSION_ID)

    private fun slots(
        regionText: String? = null,
        nearMe: Boolean = false,
        themes: List<String> = emptyList(),
        keywords: List<String> = emptyList(),
        placeName: String? = null,
        noPlace: Boolean = false,
        offTopic: Boolean = false,
    ) = ExtractedSlots(regionText, nearMe, themes, keywords, placeName, noPlace, offTopic)

    private fun course(id: Long) = AiChatCourse(id, "코스 $id", null, listOf("DATE"), "성수", 3, 20, 1, 2)

    private fun courseList(vararg ids: Long) = ids.map(::course)

    // ----- REGION -----

    @Test
    fun `첫 답변은 세션을 만들고 별칭 지역을 지역 마스터 prefix 로 풀어 테마를 묻는다`() {
        `when`(area.resolveSearchPrefixes("성동구 성수동")).thenReturn(listOf(SEONGSU_DONG))

        val result = service().sendMessage(text("성수에서 가고 싶어요", sessionId = null))

        assertThat(result.step).isEqualTo(AiChatStep.THEME)
        assertThat(result.sessionId).isNotBlank()
        assertThat(result.quickReplies).hasSize(13)
        assertThat(result.quickReplies.last().value).isEqualTo(AiChatThemeDictionary.ANY)
        assertThat(result.inputEnabled).isTrue()
        assertThat(result.inputPlaceholder).isNotBlank()
        val session = sessions.saved.getValue(result.sessionId)
        assertThat(session.userId).isEqualTo(USER_ID)
        assertThat(session.step).isEqualTo(AiChatStep.THEME)
        assertThat(session.region).isEqualTo(RegionSlot("성수", listOf(SEONGSU_DONG)))
        assertThat(llm.requests).isEmpty()
    }

    @Test
    fun `지역을 못 풀고 LLM 도 없으면 같은 단계를 칩 없이 다시 묻는다`() {
        seed(AiChatStep.REGION, region = null)

        val result = service().sendMessage(text("ㅁㄴㅇㄹ"))

        assertThat(result.step).isEqualTo(AiChatStep.REGION)
        assertThat(result.quickReplies).isEmpty()
        assertThat(result.messages.single()).contains("찾지 못했어요")
        assertThat(llm.requests.single().step).isEqualTo(AiChatStep.REGION)
        assertThat(saved().llmCalls).isEqualTo(1)
        assertThat(saved().region).isNull()
    }

    @Test
    fun `두 글자 미만 지역 입력은 지역 마스터를 찾지 않는다`() {
        seed(AiChatStep.REGION, region = null)

        val result = service().sendMessage(text("성"))

        assertThat(result.step).isEqualTo(AiChatStep.REGION)
        verify(area, never()).resolveSearchPrefixes(anyString())
    }

    @Test
    fun `LLM 이 지역·테마·장소 없음을 한 번에 꺼내면 남은 질문을 건너뛰고 바로 추천한다`() {
        seed(AiChatStep.REGION, region = null)
        `when`(area.resolveSearchPrefixes("마포구 연남동")).thenReturn(listOf(YEONNAM_DONG))
        llm.slots = slots(regionText = "연남", themes = listOf("DATE"), noPlace = true)
        courses.answer = { courseList(1, 2, 3) }

        val result = service().sendMessage(text("연남에서 데이트하고 싶은데 장소는 상관없어"))

        assertThat(result.step).isEqualTo(AiChatStep.RESULT)
        assertThat(result.courses.map { it.id }).containsExactly(1L, 2L, 3L)
        assertThat(result.inputEnabled).isFalse()
        val criteria = courses.calls.single()
        assertThat(criteria.regionPrefixes).containsExactly(YEONNAM_DONG)
        assertThat(criteria.category).isEqualTo("DATE")
        assertThat(criteria.placeId).isNull()
        assertThat(criteria.size).isEqualTo(AiChatService.COURSE_COUNT)
        assertThat(saved().region).isEqualTo(RegionSlot("연남", listOf(YEONNAM_DONG)))
        assertThat(saved().theme).isEqualTo(ThemeSlot("DATE", null, listOf("DATE")))
        assertThat(saved().shownCourseIds).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `LLM 이 꺼낸 지역 이름도 지역 마스터에 없으면 다시 묻는다`() {
        seed(AiChatStep.REGION, region = null)
        llm.slots = slots(regionText = "아틀란티스")

        val result = service().sendMessage(text("아틀란티스 가고 싶어"))

        assertThat(result.step).isEqualTo(AiChatStep.REGION)
        assertThat(result.messages.single()).contains("찾지 못했어요")
    }

    @Test
    fun `LLM 이 주제 밖이라 하면 안내 문구로 지역을 다시 묻는다`() {
        seed(AiChatStep.REGION, region = null)
        llm.slots = slots(offTopic = true)

        val result = service().sendMessage(text("오늘 날씨 어때"))

        assertThat(result.step).isEqualTo(AiChatStep.REGION)
        assertThat(result.messages.single()).contains("코스 찾기를 도와드려요")
    }

    @Test
    fun `LLM 이 근처만 말했다고 하면 내 근처 칩을 안내한다`() {
        seed(AiChatStep.REGION, region = null)
        llm.slots = slots(nearMe = true)

        val result = service().sendMessage(text("여기 근처로 해줘"))

        assertThat(result.step).isEqualTo(AiChatStep.REGION)
        assertThat(result.messages.single()).contains("내 근처")
    }

    @Test
    fun `내 근처는 좌표가 없으면 지역을 다시 묻는다`() {
        seed(AiChatStep.REGION, region = null)

        val result = service().sendMessage(chip(AiChatService.NEAR_ME))

        assertThat(result.step).isEqualTo(AiChatStep.REGION)
        assertThat(result.quickReplies).isEmpty()
        assertThat(result.messages.single()).contains("위치 권한")
    }

    @Test
    fun `내 근처는 좌표로 동을 못 찾으면 다시 묻고 찾으면 그 이름으로 테마를 묻는다`() {
        seed(AiChatStep.REGION, region = null)
        val here = Coordinate(37.5446, 127.0559)

        val failed = service().sendMessage(chip(AiChatService.NEAR_ME, location = here))
        assertThat(failed.step).isEqualTo(AiChatStep.REGION)
        assertThat(failed.messages.single()).contains("위치를 확인하지 못했어요")

        location.areaCode = SEONGSU_DONG
        `when`(area.findAreaByCode(SEONGSU_DONG))
            .thenReturn(AreaDescriptor(SEONGSU_DONG, "성수동", "서울특별시 성동구 성수동", AreaLevel.DONG))

        val result = service().sendMessage(chip(AiChatService.NEAR_ME, location = here))

        assertThat(result.step).isEqualTo(AiChatStep.THEME)
        assertThat(saved().region).isEqualTo(RegionSlot("성수동", listOf(SEONGSU_DONG)))
    }

    @Test
    fun `지역 단계에서 내 근처 외의 칩은 거절한다`() {
        seed(AiChatStep.REGION, region = null)

        assertThatThrownBy { service().sendMessage(chip("DATE")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("DATE")
    }

    // ----- THEME -----

    @Test
    fun `테마 칩은 카테고리 필터가 되고 장소를 묻는다`() {
        seed(AiChatStep.THEME)

        val result = service().sendMessage(chip("DATE"))

        assertThat(result.step).isEqualTo(AiChatStep.PLACE)
        assertThat(result.quickReplies.map { it.value }).containsExactly(AiChatService.NO_PLACE)
        assertThat(result.inputEnabled).isTrue()
        assertThat(saved().theme).isEqualTo(ThemeSlot("DATE", null, listOf("DATE")))
        assertThat(llm.requests).isEmpty()
    }

    @Test
    fun `상관없어요 칩은 테마 없이 넘어간다`() {
        seed(AiChatStep.THEME)

        service().sendMessage(chip(AiChatThemeDictionary.ANY))

        assertThat(saved().theme).isEqualTo(ThemeSlot(null, null, emptyList()))
    }

    @Test
    fun `테마 단계에 없는 칩은 거절한다`() {
        seed(AiChatStep.THEME)

        assertThatThrownBy { service().sendMessage(chip(AiChatService.NEAR_ME)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `테마 직접 입력은 LLM 이 없으면 동의어 사전으로 분류한다`() {
        seed(AiChatStep.THEME)

        val result = service().sendMessage(text("힐링하면서 쉬고 싶어"))

        assertThat(result.step).isEqualTo(AiChatStep.PLACE)
        assertThat(saved().theme).isEqualTo(ThemeSlot("HEALING", null, listOf("HEALING")))
        val request = llm.requests.single()
        assertThat(request.step).isEqualTo(AiChatStep.THEME)
        assertThat(request.regionLabel).isEqualTo("성수")
        assertThat(saved().llmCalls).isEqualTo(1)
    }

    @Test
    fun `LLM 이 테마 여러 개와 키워드를 주면 12종만 남겨 가산으로 쓴다`() {
        seed(AiChatStep.THEME)
        llm.slots = slots(themes = listOf("DATE", "CAFETOUR", "WEIRD"), keywords = listOf("실내", "가성비"))

        val result = service().sendMessage(text("실내에서 가성비 좋은 데이트 카페"))

        assertThat(result.step).isEqualTo(AiChatStep.PLACE)
        assertThat(saved().theme).isEqualTo(ThemeSlot(null, "실내 가성비", listOf("DATE", "CAFETOUR")))
    }

    @Test
    fun `LLM 이 테마도 키워드도 못 찾으면 상관없음으로 넘어간다`() {
        seed(AiChatStep.THEME)
        llm.slots = slots()

        service().sendMessage(text("음"))

        assertThat(saved().theme).isEqualTo(ThemeSlot(null, null, emptyList()))
        assertThat(saved().step).isEqualTo(AiChatStep.PLACE)
    }

    @Test
    fun `LLM 이 주제 밖이라 하면 테마 칩과 함께 다시 묻는다`() {
        seed(AiChatStep.THEME)
        llm.slots = slots(offTopic = true)

        val result = service().sendMessage(text("너 누구야"))

        assertThat(result.step).isEqualTo(AiChatStep.THEME)
        assertThat(result.quickReplies).hasSize(13)
        assertThat(result.messages.single()).contains("코스 찾기를 도와드려요")
        assertThat(saved().theme).isNull()
    }

    @Test
    fun `LLM 이 장소 이름까지 꺼내면 장소 질문을 건너뛰고 검색하되 LLM 을 다시 부르지 않는다`() {
        seed(AiChatStep.THEME)
        llm.slots = slots(themes = listOf("FOOD"), placeName = "어니언 성수")

        val result = service().sendMessage(text("맛집 코스, 어니언 성수는 꼭 가고 싶어"))

        assertThat(result.step).isEqualTo(AiChatStep.PLACE)
        assertThat(result.messages.single()).contains("찾지 못했어요")
        assertThat(places.queries).containsExactly("어니언 성수")
        assertThat(llm.requests).hasSize(1)
        assertThat(saved().theme?.category).isEqualTo("FOOD")
    }

    @Test
    fun `LLM 호출은 세션당 상한을 넘기지 않는다`() {
        seed(AiChatStep.THEME, llmCalls = 1)
        llm.slots = slots(themes = listOf("DATE"))

        service(maxCalls = 1).sendMessage(text("힐링하면서 쉬고 싶어"))

        assertThat(llm.requests).isEmpty()
        assertThat(saved().theme?.category).isEqualTo("HEALING")
        assertThat(saved().llmCalls).isEqualTo(1)
    }

    // ----- PLACE -----

    @Test
    fun `없어요 칩은 장소 없이 추천한다`() {
        seed(AiChatStep.PLACE, theme = AiChatThemeDictionary.slotOf("DATE"))
        courses.answer = { courseList(1, 2, 3) }

        val result = service().sendMessage(chip(AiChatService.NO_PLACE))

        assertThat(result.step).isEqualTo(AiChatStep.RESULT)
        assertThat(result.courses).hasSize(3)
        assertThat(result.messages.single()).isEqualTo("이런 코스는 어때요? 마음에 들면 저장해 두세요.")
        assertThat(result.quickReplies.map { it.value })
            .containsExactly(AiChatService.RETRY, AiChatService.RESTART)
        assertThat(courses.calls.single().placeId).isNull()
        assertThat(courses.calls.single().likeThemes).containsExactly("DATE")
    }

    @Test
    fun `장소 단계에 없는 칩은 거절한다`() {
        seed(AiChatStep.PLACE)

        assertThatThrownBy { service().sendMessage(chip("DATE")) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `장소 후보가 하나면 바로 그 장소가 든 코스를 추천한다`() {
        seed(AiChatStep.PLACE)
        places.answer = { listOf(onion) }
        courses.answer = { courseList(7) }

        val result = service().sendMessage(text("어니언"))

        assertThat(result.step).isEqualTo(AiChatStep.RESULT)
        assertThat(places.queries).containsExactly("어니언")
        assertThat(courses.calls.first().placeId).isEqualTo(10L)
        assertThat(saved().place).isEqualTo(PlaceSlot(10L, "어니언 성수"))
        assertThat(llm.requests).isEmpty()
    }

    @Test
    fun `후보가 여러 개면 어느 곳인지 칩으로 되묻는다`() {
        seed(AiChatStep.PLACE)
        places.answer = { listOf(onion, onionAnguk) }

        val result = service().sendMessage(text("어니언"))

        assertThat(result.step).isEqualTo(AiChatStep.PLACE_CONFIRM)
        assertThat(result.inputEnabled).isFalse()
        assertThat(result.quickReplies.map { it.label })
            .containsExactly("어니언 성수 · 카페·디저트 · 성수동2가", "어니언 안국 · 카페·디저트 · 계동", "이 중에 없어요")
        assertThat(result.quickReplies.map { it.value })
            .containsExactly("place:10", "place:11", AiChatService.PLACE_NOT_LISTED)
        assertThat(saved().placeCandidates).containsOnlyKeys("place:10", "place:11")
        assertThat(courses.calls).isEmpty()
    }

    @Test
    fun `후보 중 띄어쓰기만 다른 같은 이름이 하나면 바로 확정한다`() {
        seed(AiChatStep.PLACE)
        places.answer = { listOf(onion, onionAnguk) }
        courses.answer = { courseList(7) }

        val result = service().sendMessage(text("어니언성수"))

        assertThat(result.step).isEqualTo(AiChatStep.RESULT)
        assertThat(saved().place).isEqualTo(PlaceSlot(10L, "어니언 성수"))
    }

    @Test
    fun `장소를 못 찾으면 LLM 이 꺼낸 이름으로 한 번만 더 찾는다`() {
        seed(AiChatStep.PLACE)
        places.answer = { name -> if (name == "어니언") listOf(onion) else emptyList() }
        llm.slots = slots(placeName = "어니언")
        courses.answer = { courseList(7) }

        val result = service().sendMessage(text("성수 어니언 가고싶어"))

        assertThat(result.step).isEqualTo(AiChatStep.RESULT)
        assertThat(places.queries).containsExactly("성수 어니언 가고싶어", "어니언")
        assertThat(llm.requests.single().step).isEqualTo(AiChatStep.PLACE)
        assertThat(courses.calls.first().placeId).isEqualTo(10L)
    }

    @Test
    fun `장소 없다는 문장은 LLM 이 noPlace 로 알려 바로 추천한다`() {
        seed(AiChatStep.PLACE)
        llm.slots = slots(noPlace = true)
        courses.answer = { courseList(1) }

        val result = service().sendMessage(text("딱히 없어 그냥 추천해줘"))

        assertThat(result.step).isEqualTo(AiChatStep.RESULT)
        assertThat(saved().place).isNull()
    }

    @Test
    fun `장소를 못 찾고 LLM 도 없거나 주제 밖이면 장소를 다시 묻는다`() {
        seed(AiChatStep.PLACE)

        val notFound = service().sendMessage(text("없는가게"))
        assertThat(notFound.step).isEqualTo(AiChatStep.PLACE)
        assertThat(notFound.messages.single()).contains("찾지 못했어요")
        assertThat(notFound.quickReplies.map { it.value }).containsExactly(AiChatService.NO_PLACE)

        llm.slots = slots(offTopic = true)
        val offTopic = service().sendMessage(text("ㅋㅋㅋ"))
        assertThat(offTopic.step).isEqualTo(AiChatStep.PLACE)
        assertThat(offTopic.messages.single()).contains("코스 찾기를 도와드려요")
    }

    // ----- PLACE_CONFIRM -----

    private fun seedConfirm() =
        seed(
            AiChatStep.PLACE_CONFIRM,
            candidates = mapOf("place:10" to PlaceSlot(10L, "어니언 성수"), "place:11" to PlaceSlot(11L, "어니언 안국")),
        )

    @Test
    fun `장소 확인 단계는 칩만 받고 고른 장소로 추천한다`() {
        seedConfirm()
        courses.answer = { courseList(7) }

        assertThatThrownBy { service().sendMessage(text("안국")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("칩으로만")

        val result = service().sendMessage(chip("place:11"))

        assertThat(result.step).isEqualTo(AiChatStep.RESULT)
        assertThat(courses.calls.first().placeId).isEqualTo(11L)
        assertThat(saved().place).isEqualTo(PlaceSlot(11L, "어니언 안국"))
        assertThat(saved().placeCandidates).isEmpty()
    }

    @Test
    fun `이 중에 없어요는 장소를 다시 묻고 후보에 없는 칩은 거절한다`() {
        seedConfirm()

        val result = service().sendMessage(chip(AiChatService.PLACE_NOT_LISTED))
        assertThat(result.step).isEqualTo(AiChatStep.PLACE)
        assertThat(result.messages.single()).contains("다른 이름")
        assertThat(saved().placeCandidates).isEmpty()

        seedConfirm()
        assertThatThrownBy { service().sendMessage(chip("place:99")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("place:99")
    }

    // ----- RESULT -----

    @Test
    fun `더 추천받기는 이미 보여 준 코스를 빼고 다음 묶음을 찾는다`() {
        seed(AiChatStep.RESULT, theme = AiChatThemeDictionary.slotOf("DATE"), shown = listOf(1L, 2L, 3L))
        courses.answer = { courseList(4, 5, 6) }

        val result = service().sendMessage(chip(AiChatService.RETRY))

        assertThat(result.step).isEqualTo(AiChatStep.RESULT)
        assertThat(result.courses.map { it.id }).containsExactly(4L, 5L, 6L)
        assertThat(result.messages.single()).contains("이런 코스도 있어요")
        assertThat(courses.calls.single().excludeIds).containsExactly(1L, 2L, 3L)
        assertThat(saved().shownCourseIds).containsExactly(1L, 2L, 3L, 4L, 5L, 6L)
    }

    @Test
    fun `더 보여줄 코스가 없으면 처음부터 칩만 남긴다`() {
        seed(AiChatStep.RESULT, shown = listOf(1L, 2L, 3L))

        val result = service().sendMessage(chip(AiChatService.RETRY))

        assertThat(result.courses).isEmpty()
        assertThat(result.messages.single()).contains("더 보여드릴 코스가 없어요")
        assertThat(result.quickReplies.map { it.value }).containsExactly(AiChatService.RESTART)
        assertThat(result.inputEnabled).isFalse()
        assertThat(saved().shownCourseIds).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `결과 단계에서 더 추천받기 외 칩이나 직접 입력은 거절한다`() {
        seed(AiChatStep.RESULT)

        assertThatThrownBy { service().sendMessage(text("더")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { service().sendMessage(chip(AiChatService.NO_PLACE)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    // ----- 조건 완화 -----

    /** 장소·테마·동 지역이 모두 찬 상태로 추천을 돌려, [succeedAt] 번째 시도에서만 결과가 나오게 한다. */
    private fun recommendSucceedingAt(succeedAt: Int): AiChatTurnResult {
        courses.calls.clear()
        seed(AiChatStep.PLACE, theme = AiChatThemeDictionary.slotOf("DATE"))
        places.answer = { listOf(onion) }
        courses.answer = { if (courses.calls.size == succeedAt) courseList(9) else emptyList() }
        return service().sendMessage(text("어니언"))
    }

    private fun attempt(index: Int) = courses.calls[index]

    @Test
    fun `완화 사다리는 장소 제거 → 테마 완화 → 시군구 → 시도 → 인기 순으로 조건을 푼다`() {
        val result = recommendSucceedingAt(6)

        assertThat(result.messages.single()).contains("인기 코스")
        assertThat(courses.calls).hasSize(6)
        assertThat(attempt(0)).matches {
            it.placeId == 10L && it.category == "DATE" &&
                it.regionPrefixes == listOf(SEONGSU_DONG)
        }
        assertThat(attempt(1)).matches {
            it.placeId == null && it.category == "DATE" &&
                it.regionPrefixes == listOf(SEONGSU_DONG)
        }
        assertThat(attempt(2)).matches {
            it.placeId == null && it.category == null &&
                it.regionPrefixes == listOf(SEONGSU_DONG)
        }
        assertThat(attempt(3).regionPrefixes).containsExactly("11200")
        assertThat(attempt(4).regionPrefixes).containsExactly("11")
        assertThat(attempt(5).regionPrefixes).isEmpty()
        // 완화 중에도 가산 조건(관심 테마)과 제외 목록은 그대로 간다.
        assertThat(courses.calls.map { it.likeThemes }).allMatch { it == listOf("DATE") }
    }

    @Test
    fun `좁은 조건 결과가 3개 미만이면 다음 단계에서 부족한 만큼 채우고 좁은 결과를 앞에 둔다`() {
        seed(AiChatStep.PLACE, theme = AiChatThemeDictionary.slotOf("DATE"))
        courses.answer = { if (courses.calls.size == 1) courseList(1) else courseList(2, 3) }

        val result = service().sendMessage(chip(AiChatService.NO_PLACE))

        assertThat(result.courses.map { it.id }).containsExactly(1L, 2L, 3L)
        assertThat(result.messages.single()).isEqualTo("딱 맞는 테마의 코스가 적어서 비슷한 분위기의 코스도 함께 골라봤어요.")
        assertThat(courses.calls).hasSize(2)
        assertThat(attempt(0)).matches { it.category == "DATE" && it.size == 3 && it.excludeIds.isEmpty() }
        assertThat(attempt(1)).matches { it.category == null && it.size == 2 && it.excludeIds == listOf(1L) }
        assertThat(saved().shownCourseIds).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `채우기는 가장 많이 푼 단계 기준으로 알리고 더 추천받기의 제외 목록과 합쳐진다`() {
        seed(AiChatStep.RESULT, theme = AiChatThemeDictionary.slotOf("DATE"), shown = listOf(9L))
        courses.answer =
            {
                if (courses.calls.size ==
                    1
                ) {
                    courseList(1)
                } else if (courses.calls.size == 5) {
                    courseList(2)
                } else {
                    emptyList()
                }
            }

        val result = service().sendMessage(chip(AiChatService.RETRY))

        assertThat(result.courses.map { it.id }).containsExactly(1L, 2L)
        assertThat(result.messages.single()).isEqualTo("조건에 맞는 코스가 적어서 인기 코스도 함께 골라봤어요.")
        assertThat(courses.calls).hasSize(5)
        assertThat(
            attempt(4),
        ).matches { it.regionPrefixes.isEmpty() && it.size == 2 && it.excludeIds == listOf(9L, 1L) }
    }

    @Test
    fun `3개가 다 차면 더 넓히지 않는다`() {
        seed(AiChatStep.PLACE, theme = AiChatThemeDictionary.slotOf("DATE"))
        courses.answer = { courseList(1, 2, 3) }

        val result = service().sendMessage(chip(AiChatService.NO_PLACE))

        assertThat(result.courses).hasSize(3)
        assertThat(result.messages.single()).isEqualTo("이런 코스는 어때요? 마음에 들면 저장해 두세요.")
        assertThat(courses.calls).hasSize(1)
    }

    @Test
    fun `장소를 풀어 찾았으면 그렇게 알린다`() {
        assertThat(recommendSucceedingAt(2).messages.single()).contains("그 장소가 들어간 코스는 아직 없어요")
    }

    @Test
    fun `테마를 풀어 찾았으면 그렇게 알린다`() {
        assertThat(recommendSucceedingAt(3).messages.single()).contains("딱 맞는 테마의 코스가 없어서")
    }

    @Test
    fun `지역을 넓혀 찾았으면 지역 이름과 함께 알린다`() {
        assertThat(recommendSucceedingAt(4).messages.single()).isEqualTo("성수 안에는 코스가 적어서 주변까지 넓혀 봤어요.")
        assertThat(recommendSucceedingAt(5).messages.single()).contains("넓혀 봤어요")
    }

    @Test
    fun `시군구 단위 지역은 시도까지만 넓히고 테마가 없으면 테마 완화 단계가 없다`() {
        seed(AiChatStep.PLACE, region = RegionSlot("성동구", listOf("11200")), theme = AiChatThemeDictionary.slotOf(null))

        service().sendMessage(chip(AiChatService.NO_PLACE))

        assertThat(courses.calls.map { it.regionPrefixes }).containsExactly(listOf("11200"), listOf("11"), emptyList())
    }

    @Test
    fun `내 근처 없이 지역이 비어 있으면 전체에서 한 번만 찾는다`() {
        seed(AiChatStep.PLACE, region = null, theme = AiChatThemeDictionary.slotOf(null))

        val result = service().sendMessage(chip(AiChatService.NO_PLACE))

        assertThat(courses.calls.map { it.regionPrefixes }).containsExactly(emptyList())
        assertThat(result.messages.single()).contains("더 보여드릴 코스가 없어요")
    }

    // ----- 세션 -----

    @Test
    fun `세션이 없거나 다른 사용자의 세션이면 4400 이다`() {
        assertThatThrownBy { service().sendMessage(text("성수", sessionId = "missing")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AiChatErrorCode.AI_CHAT_SESSION_NOT_FOUND)

        seed(AiChatStep.THEME, userId = 99L)
        assertThatThrownBy { service().sendMessage(chip("DATE")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AiChatErrorCode.AI_CHAT_SESSION_NOT_FOUND)
    }

    @Test
    fun `빈 sessionId 는 새 대화로 본다`() {
        `when`(area.resolveSearchPrefixes("성동구")).thenReturn(listOf("11200"))

        val result = service().sendMessage(text("성동구", sessionId = "  "))

        assertThat(result.step).isEqualTo(AiChatStep.THEME)
        assertThat(result.sessionId).isNotBlank()
        assertThat(sessions.saved.getValue(result.sessionId).region).isEqualTo(RegionSlot("성동구", listOf("11200")))
    }

    // ----- fakes -----

    private class FakeSessionPort : AiChatSessionPort {
        val saved = mutableMapOf<String, AiChatSession>()

        override fun find(sessionId: String): AiChatSession? = saved[sessionId]

        override fun save(session: AiChatSession) {
            saved[session.id] = session
        }
    }

    private class FakeCoursePort : AiChatCoursePort {
        val calls = mutableListOf<AiChatCourseCriteria>()
        var answer: (AiChatCourseCriteria) -> List<AiChatCourse> = { emptyList() }

        override fun recommend(criteria: AiChatCourseCriteria): List<AiChatCourse> {
            calls += criteria
            return answer(criteria)
        }
    }

    private class FakePlacePort : AiChatPlacePort {
        val queries = mutableListOf<String>()
        var answer: (String) -> List<AiChatPlace> = { emptyList() }

        override fun searchByName(
            name: String,
            size: Int,
        ): List<AiChatPlace> {
            queries += name
            return answer(name)
        }
    }

    private class FakeLocationPort : AiChatLocationPort {
        var areaCode: String? = null

        override fun findAreaCodeAt(location: Coordinate): String? = areaCode
    }

    private class FakeSlotPort : AiChatSlotExtractionPort {
        val requests = mutableListOf<SlotExtractionRequest>()
        var slots: ExtractedSlots? = null

        override fun extract(request: SlotExtractionRequest): ExtractedSlots? {
            requests += request
            return slots
        }
    }

    private companion object {
        const val USER_ID = 1L
        const val SESSION_ID = "s1"
        const val SEONGSU_DONG = "1120011400"
        const val YEONNAM_DONG = "1144012000"
    }
}
