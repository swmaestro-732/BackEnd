package com.example.backend.mobile.aichat.adapter.outbound

import com.anthropic.client.AnthropicClient
import com.example.backend.bootstrap.config.AiChatLlmProperties
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatStep
import com.example.backend.mobile.aichat.application.port.outbound.ExtractedSlots
import com.example.backend.mobile.aichat.application.port.outbound.SlotExtractionRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider

/** [AnthropicSlotExtractionAdapter] — 클라이언트 부재·호출 실패의 fail-soft(null)와 구조화 출력 → 슬롯 정규화를 검증한다. */
class AnthropicSlotExtractionAdapterTest {
    private val request = SlotExtractionRequest(AiChatStep.THEME, "조용한 데이트", regionLabel = "성수", themeCategory = null)

    @Suppress("UNCHECKED_CAST")
    private fun providerOf(client: AnthropicClient?): ObjectProvider<AnthropicClient> {
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<AnthropicClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    @Test
    fun `클라이언트가 없으면(키 미설정·기능 끔) null`() {
        assertThat(AnthropicSlotExtractionAdapter(providerOf(null), AiChatLlmProperties()).extract(request)).isNull()
    }

    @Test
    fun `호출이 예외로 끝나면 null 로 흡수한다`() {
        val client = mock(AnthropicClient::class.java)
        `when`(client.messages()).thenThrow(RuntimeException("timeout"))

        assertThat(AnthropicSlotExtractionAdapter(providerOf(client), AiChatLlmProperties()).extract(request)).isNull()
    }

    @Test
    fun `구조화 출력은 공백을 다듬고 테마는 대문자·중복 제거, 빈 문자열은 null 로 바꾼다`() {
        val output =
            AnthropicSlotExtractionAdapter.SlotOutput().apply {
                regionText = " 성수 "
                nearMe = false
                themes = listOf("date", " DATE", "CafeTour", "")
                themeKeywords = listOf(" 실내 ", "", "실내")
                placeName = "  "
                noPlace = true
                offTopic = false
            }

        assertThat(output.toSlots()).isEqualTo(
            ExtractedSlots(
                regionText = "성수",
                nearMe = false,
                themes = listOf("DATE", "CAFETOUR"),
                themeKeywords = listOf("실내"),
                placeName = null,
                noPlace = true,
                offTopic = false,
            ),
        )
        assertThat(AnthropicSlotExtractionAdapter.SlotOutput().toSlots())
            .isEqualTo(ExtractedSlots(null, false, emptyList(), emptyList(), null, false, false))
    }
}
