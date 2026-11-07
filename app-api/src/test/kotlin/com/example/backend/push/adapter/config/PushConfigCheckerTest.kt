package com.example.backend.push.adapter.config

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class PushConfigCheckerTest {
    @Test
    fun `큐와 키가 모두 있거나 모두 없으면 경고하지 않는다`() {
        assertNull(pushConfigMismatchMessage(QUEUE_URL, KEY_JSON))
        assertNull(pushConfigMismatchMessage("", ""))
        assertNull(pushConfigMismatchMessage(" ", " "))
    }

    @Test
    fun `큐만 있으면 푸시가 큐에 쌓인다고 알리고 키 값은 넣지 않는다`() {
        val message = pushConfigMismatchMessage(QUEUE_URL, " ")!!

        assertTrue(message.contains("FIREBASE_CREDENTIALS_JSON"))
        assertTrue(message.contains("쌓입니다"))
    }

    @Test
    fun `키만 있으면 발행을 생략한다고 알리고 키 값은 넣지 않는다`() {
        val message = pushConfigMismatchMessage("", KEY_JSON)!!

        assertTrue(message.contains("SQS_PUSH_QUEUE_URL"))
        assertFalse(message.contains("secret-key"))
    }

    @Test
    fun `기동 점검은 따옴표가 든 키에서도 예외 없이 끝난다`() {
        val environment = MockEnvironment().withProperty("firebase.credentials-json", KEY_JSON)
        val checker = PushConfigChecker(PushQueueProperties(), environment)

        assertDoesNotThrow { checker.check() }
    }

    @Test
    fun `키 설정이 없어도 기동 점검은 예외 없이 끝난다`() {
        val checker = PushConfigChecker(PushQueueProperties(pushQueueUrl = QUEUE_URL), MockEnvironment())

        assertDoesNotThrow { checker.check() }
    }

    private companion object {
        const val QUEUE_URL = "https://sqs.ap-northeast-2.amazonaws.com/123/push"
        const val KEY_JSON = """{"type":"it's","private_key":"secret-key"}"""
    }
}
