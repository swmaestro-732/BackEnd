package com.example.backend.push.application.port.inbound.dto

import com.example.backend.push.application.port.outbound.PushSendResult
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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

    @ParameterizedTest
    @ValueSource(strings = ["from", "notification", "message_type", "FROM", "Message_Type"])
    fun `FCM 예약 data 키는 대소문자와 관계없이 거부한다`(key: String) {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                PushMessage(listOf("token-1"), "제목", "본문", mapOf(key to "value"))
            }

        assertTrue(thrown.message!!.contains(key))
    }

    @ParameterizedTest
    @ValueSource(strings = ["google.c.a.e", "gcm.notification.title", "Google.sent_time", "GCM.n.e"])
    fun `google 과 gcm 접두사 data 키는 대소문자와 관계없이 거부한다`(key: String) {
        assertThrows(IllegalArgumentException::class.java) {
            PushMessage(listOf("token-1"), "제목", "본문", mapOf(key to "value"))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["type", "fromUser", "googleId", "courseId"])
    fun `예약어와 접두사가 아닌 data 키는 허용한다`(key: String) {
        assertDoesNotThrow { PushMessage(listOf("token-1"), "제목", "본문", mapOf(key to "value")) }
    }

    @Test
    fun `제목 본문 data 의 UTF-8 크기 합이 4000 bytes 면 허용한다`() {
        // 한글 1자는 3 bytes. 제목 3 + 본문 3 + 키 4 + 값 3990 = 4000
        assertDoesNotThrow { PushMessage(listOf("token-1"), "제", "본", mapOf("type" to "a".repeat(3990))) }
    }

    @Test
    fun `제목 본문 data 의 UTF-8 크기 합이 4000 bytes 를 넘으면 거부한다`() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                PushMessage(listOf("token-1"), "제", "본", mapOf("type" to "a".repeat(3991)))
            }

        assertTrue(thrown.message!!.contains("bytes=4001"))
    }

    @Test
    fun `한글 본문은 글자 수가 아니라 UTF-8 bytes 로 크기를 잰다`() {
        assertThrows(IllegalArgumentException::class.java) {
            PushMessage(listOf("token-1"), "제목", "가".repeat(1400))
        }
    }
}
