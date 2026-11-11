package com.example.backend.mobile.aichat.application.service

import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.ThemeSlot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** [AiChatThemeDictionary] — 칩 목록과 직접 입력 분류(무관심·단일·다중·미일치)를 검증한다. */
class AiChatThemeDictionaryTest {
    @Test
    fun `칩은 카테고리 12종 뒤에 상관없어요가 붙는다`() {
        val chips = AiChatThemeDictionary.CHIPS

        assertThat(chips).hasSize(13)
        assertThat(chips.dropLast(1).map { it.value }).containsExactlyElementsOf(AiChatThemeDictionary.CATEGORIES)
        assertThat(chips.first()).matches { it.label == "데이트" && it.value == "DATE" && !it.requiresLocation }
        assertThat(chips.last().value).isEqualTo(AiChatThemeDictionary.ANY)
    }

    @Test
    fun `동의어가 하나만 걸리면 그 카테고리 필터다`() {
        assertThat(AiChatThemeDictionary.classify("조용히 걷고 싶어요"))
            .isEqualTo(ThemeSlot("HEALING", null, listOf("HEALING")))
        assertThat(AiChatThemeDictionary.classify("부모님이랑 나들이")).isEqualTo(ThemeSlot("FAMILY", null, listOf("FAMILY")))
    }

    @Test
    fun `동의어가 여러 카테고리에 걸리면 필터 없이 가산 목록만 둔다`() {
        val slot = AiChatThemeDictionary.classify("혼자 카페 투어")

        assertThat(slot.category).isNull()
        assertThat(slot.keyword).isNull()
        assertThat(slot.likeThemes).containsExactlyInAnyOrder("CAFETOUR", "SOLO")
    }

    @Test
    fun `사전에 없으면 문장을 키워드로 넘긴다`() {
        assertThat(AiChatThemeDictionary.classify("  비 오는 날 실내  ")).isEqualTo(ThemeSlot(null, "비 오는 날 실내", emptyList()))
    }

    @Test
    fun `무관심 표현은 동의어가 섞여 있어도 상관없음이다`() {
        assertThat(AiChatThemeDictionary.classify("아무 카페나 상관없어요")).isEqualTo(AiChatThemeDictionary.slotOf(null))
        assertThat(AiChatThemeDictionary.slotOf(null)).isEqualTo(ThemeSlot(null, null, emptyList()))
        assertThat(AiChatThemeDictionary.slotOf("FOOD")).isEqualTo(ThemeSlot("FOOD", null, listOf("FOOD")))
    }
}
