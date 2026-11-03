package com.example.backend.push.adapter.messaging

import com.example.backend.push.application.port.inbound.dto.PushMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class PushQueueMessageTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `큐 직렬화 왕복에서 토큰과 알림 내용과 데이터가 유지된다`() {
        val message = PushMessage(listOf("token-1", "token-2"), "제목", "본문", mapOf("type" to "test"))

        val json = objectMapper.writeValueAsString(PushQueueMessage(message))
        val restored = objectMapper.readValue<PushQueueMessage>(json)

        assertEquals(message, restored.toPushMessage())
    }

    @Test
    fun `data가 없는 큐 메시지는 빈 맵으로 읽는다`() {
        val json = """{"tokens":["token-1"],"title":"제목","body":"본문"}"""

        val restored = objectMapper.readValue<PushQueueMessage>(json)

        assertEquals(PushMessage(listOf("token-1"), "제목", "본문"), restored.toPushMessage())
    }

    @Test
    fun `큐 메시지 문자열에도 토큰 값이 노출되지 않는다`() {
        val message = PushQueueMessage(listOf("secret-token"), "제목", "본문")

        assertFalse(message.toString().contains("secret-token"))
        assertTrue(message.toString().contains("tokenCount=1"))
    }
}
