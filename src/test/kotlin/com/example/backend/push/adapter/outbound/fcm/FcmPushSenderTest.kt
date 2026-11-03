package com.example.backend.push.adapter.outbound.fcm

import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.outbound.PushSendResult
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.SendResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.support.DefaultListableBeanFactory

class FcmPushSenderTest {
    @Test
    fun `등록 해제와 발신자 불일치 오류를 무효 토큰으로 분류한다`() {
        assertTrue(isInvalidToken(MessagingErrorCode.UNREGISTERED))
        assertTrue(isInvalidToken(MessagingErrorCode.SENDER_ID_MISMATCH))
    }

    @Test
    fun `나머지 오류와 알 수 없는 오류는 무효 토큰으로 분류하지 않는다`() {
        val codes =
            listOf(
                MessagingErrorCode.INTERNAL,
                MessagingErrorCode.UNAVAILABLE,
                MessagingErrorCode.QUOTA_EXCEEDED,
                MessagingErrorCode.INVALID_ARGUMENT,
                MessagingErrorCode.THIRD_PARTY_AUTH_ERROR,
                null,
            )
        for (code in codes) {
            assertFalse(isInvalidToken(code), "code=$code")
        }
    }

    @Test
    fun `Firebase 미설정이면 발송을 생략하고 모든 결과를 0으로 반환한다`() {
        val firebaseMessagingProvider = DefaultListableBeanFactory().getBeanProvider(FirebaseMessaging::class.java)
        val fcmPushSender = FcmPushSender(firebaseMessagingProvider)

        val result = fcmPushSender.send(PushMessage(listOf("secret-token"), "제목", "본문"))

        assertEquals(PushSendResult(0, emptyList(), 0), result)
    }

    @Test
    fun `개별 응답 순서에 맞춰 무효 토큰과 기타 실패를 분류한다`() {
        val firebaseMessaging = mock(FirebaseMessaging::class.java)
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("firebaseMessaging", firebaseMessaging)
        val fcmPushSender = FcmPushSender(beanFactory.getBeanProvider(FirebaseMessaging::class.java))
        val success = mock(SendResponse::class.java)
        `when`(success.isSuccessful).thenReturn(true)
        val batchResponse = mock(BatchResponse::class.java)
        `when`(batchResponse.successCount).thenReturn(1)
        val responses =
            listOf(
                success,
                failedResponse(MessagingErrorCode.UNREGISTERED),
                failedResponse(MessagingErrorCode.UNAVAILABLE),
                failedResponse(MessagingErrorCode.SENDER_ID_MISMATCH),
                failedResponse(MessagingErrorCode.INVALID_ARGUMENT),
                failedResponse(null),
            )
        `when`(batchResponse.responses).thenReturn(responses)
        `when`(firebaseMessaging.sendEachForMulticast(any(MulticastMessage::class.java))).thenReturn(batchResponse)

        val result =
            fcmPushSender.send(
                PushMessage(
                    listOf("ok", "unregistered", "unavailable", "mismatch", "bad-payload", "unknown"),
                    "제목",
                    "본문",
                ),
            )

        assertEquals(PushSendResult(1, listOf("unregistered", "mismatch"), 3), result)
    }

    @Test
    fun `FCM 호출 전체의 예외는 재시도를 위해 전파한다`() {
        val firebaseMessaging = mock(FirebaseMessaging::class.java)
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("firebaseMessaging", firebaseMessaging)
        val fcmPushSender = FcmPushSender(beanFactory.getBeanProvider(FirebaseMessaging::class.java))
        val failure = mock(FirebaseMessagingException::class.java)
        `when`(firebaseMessaging.sendEachForMulticast(any(MulticastMessage::class.java))).thenThrow(failure)

        val thrown =
            assertThrows(FirebaseMessagingException::class.java) {
                fcmPushSender.send(PushMessage(listOf("token-1"), "제목", "본문"))
            }

        assertSame(failure, thrown)
    }

    private fun failedResponse(code: MessagingErrorCode?): SendResponse {
        val exception = mock(FirebaseMessagingException::class.java)
        `when`(exception.messagingErrorCode).thenReturn(code)
        val response = mock(SendResponse::class.java)
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.exception).thenReturn(exception)
        return response
    }
}
