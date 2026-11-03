package com.example.backend.push.application.port.inbound.dto

import com.example.backend.push.application.port.outbound.PushSendResult
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PushMessageTest {
    @Test
    fun `문자열 표현은 토큰 값 대신 개수만 표시한다`() {
        val message = PushMessage(listOf("secret-token-one", "secret-token-two"), "제목", "본문")

        val text = message.toString()

        assertFalse(text.contains("secret-token-one"))
        assertFalse(text.contains("secret-token-two"))
        assertTrue(text.contains("tokenCount=2"))
    }

    @Test
    fun `발송 결과 문자열은 무효 토큰 값 대신 개수만 표시한다`() {
        val text = PushSendResult(1, listOf("secret-invalid-token"), 0).toString()

        assertFalse(text.contains("secret-invalid-token"))
        assertTrue(text.contains("invalidTokenCount=1"))
    }
}
