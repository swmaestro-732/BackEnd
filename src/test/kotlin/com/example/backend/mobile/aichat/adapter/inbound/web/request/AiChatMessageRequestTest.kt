package com.example.backend.mobile.aichat.adapter.inbound.web.request

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatInputType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** [AiChatMessageRequest] — 타입별 필수 페이로드 검증과 커맨드 변환(공백 제거·좌표)을 검증한다. */
class AiChatMessageRequestTest {
    @Test
    fun `CHIP 은 value, TEXT 는 text 가 있어야 하고 type 이 없으면 NotNull 에 맡긴다`() {
        assertThat(AiChatMessageRequest(type = AiChatMessageRequest.Type.CHIP, value = "DATE").payloadPresent).isTrue()
        assertThat(AiChatMessageRequest(type = AiChatMessageRequest.Type.CHIP, text = "성수").payloadPresent).isFalse()
        assertThat(AiChatMessageRequest(type = AiChatMessageRequest.Type.TEXT, text = "성수").payloadPresent).isTrue()
        assertThat(AiChatMessageRequest(type = AiChatMessageRequest.Type.TEXT, text = "   ").payloadPresent).isFalse()
        assertThat(AiChatMessageRequest(type = null).payloadPresent).isTrue()
    }

    @Test
    fun `커맨드로 바꿀 때 값을 다듬고 좌표를 옮긴다`() {
        val command =
            AiChatMessageRequest(
                sessionId = "s1",
                type = AiChatMessageRequest.Type.CHIP,
                value = " NEAR_ME ",
                location = AiChatMessageRequest.Location(37.5446, 127.0559),
            ).toCommand(userId = 7L)

        assertThat(command.userId).isEqualTo(7L)
        assertThat(command.sessionId).isEqualTo("s1")
        assertThat(command.type).isEqualTo(AiChatInputType.CHIP)
        assertThat(command.value).isEqualTo("NEAR_ME")
        assertThat(command.text).isNull()
        assertThat(command.location).isEqualTo(Coordinate(37.5446, 127.0559))
    }

    @Test
    fun `TEXT 는 문장만 다듬어 넘기고 좌표가 없으면 null 이다`() {
        val command = AiChatMessageRequest(type = AiChatMessageRequest.Type.TEXT, text = " 성수에서 ").toCommand(1L)

        assertThat(command.type).isEqualTo(AiChatInputType.TEXT)
        assertThat(command.text).isEqualTo("성수에서")
        assertThat(command.sessionId).isNull()
        assertThat(command.location).isNull()
    }
}
