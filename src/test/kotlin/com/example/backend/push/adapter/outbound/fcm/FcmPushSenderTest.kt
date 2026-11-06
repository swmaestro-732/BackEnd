package com.example.backend.push.adapter.outbound.fcm

import com.example.backend.bootstrap.config.PushProperties
import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.outbound.PushSendResult
import com.google.api.client.json.gson.GsonFactory
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.SendResponse
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

class FcmPushSenderTest {
    private val meterRegistry = SimpleMeterRegistry()

    @Test
    fun `등록 해제 오류만 무효 토큰으로 분류한다`() {
        assertTrue(isInvalidToken(MessagingErrorCode.UNREGISTERED))
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
                // 서버 키 설정 문제라 토큰을 지우면 안 된다.
                MessagingErrorCode.SENDER_ID_MISMATCH,
                null,
            )
        for (code in codes) {
            assertFalse(isInvalidToken(code), "code=$code")
        }
    }

    @Test
    fun `Firebase 미설정이면 발송을 생략하고 토큰 수만큼 생략 카운터를 올린다`() {
        val firebaseMessagingProvider = DefaultListableBeanFactory().getBeanProvider(FirebaseMessaging::class.java)
        val fcmPushSender = FcmPushSender(firebaseMessagingProvider, PushProperties(), meterRegistry)

        val result = fcmPushSender.send(PushMessage(listOf("secret-token", "token-2"), "제목", "본문"))

        assertEquals(PushSendResult(0, emptyList(), 0), result)
        assertEquals(
            2.0,
            meterRegistry
                .get("push.skipped")
                .tag("reason", "firebase_not_configured")
                .counter()
                .count(),
        )
    }

    @Test
    fun `안드로이드 설정에 보관 기간과 활동 알림 채널을 담는다`() {
        val config = androidConfig(Duration.ofDays(1), "courmy_activity")

        // FCM SDK 는 필드를 감춰 두므로 요청 JSON 으로 직렬화되는 값을 확인한다.
        val json = JsonMapper.builder().build().readTree(GsonFactory.getDefaultInstance().toString(config))

        assertEquals("86400s", json.get("ttl").asString())
        assertEquals("courmy_activity", json.get("notification").get("channel_id").asString())
    }

    @Test
    fun `개별 응답 순서에 맞춰 무효 토큰과 기타 실패를 분류한다`() {
        val firebaseMessaging = mock(FirebaseMessaging::class.java)
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("firebaseMessaging", firebaseMessaging)
        val fcmPushSender =
            FcmPushSender(beanFactory.getBeanProvider(FirebaseMessaging::class.java), PushProperties(), meterRegistry)
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

        assertEquals(PushSendResult(1, listOf("unregistered"), 4), result)
    }

    @Test
    fun `FCM 호출 전체의 예외는 재시도를 위해 전파한다`() {
        val firebaseMessaging = mock(FirebaseMessaging::class.java)
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("firebaseMessaging", firebaseMessaging)
        val fcmPushSender =
            FcmPushSender(beanFactory.getBeanProvider(FirebaseMessaging::class.java), PushProperties(), meterRegistry)
        val failure = mock(FirebaseMessagingException::class.java)
        `when`(firebaseMessaging.sendEachForMulticast(any(MulticastMessage::class.java))).thenThrow(failure)

        val thrown =
            assertThrows(FirebaseMessagingException::class.java) {
                fcmPushSender.send(PushMessage(listOf("token-1"), "제목", "본문"))
            }

        assertSame(failure, thrown)
    }

    @Test
    fun `일시 오류와 코드 없는 오류만 재시도 가능으로 본다`() {
        for (code in listOf(
            MessagingErrorCode.UNAVAILABLE,
            MessagingErrorCode.INTERNAL,
            MessagingErrorCode.QUOTA_EXCEEDED,
            null,
        )) {
            assertTrue(isRetryable(code), "code=$code")
        }
        for (
        code in listOf(
            MessagingErrorCode.INVALID_ARGUMENT,
            MessagingErrorCode.THIRD_PARTY_AUTH_ERROR,
            MessagingErrorCode.SENDER_ID_MISMATCH,
            MessagingErrorCode.UNREGISTERED,
        )
        ) {
            assertFalse(isRetryable(code), "code=$code")
        }
    }

    @Test
    fun `아무도 받지 못하고 실패가 모두 일시 오류면 재전송을 위해 예외를 던진다`() {
        val fcmPushSender =
            senderReturning(
                0,
                failedResponse(MessagingErrorCode.UNAVAILABLE),
                failedResponse(MessagingErrorCode.INTERNAL),
                failedResponse(null),
            )

        assertThrows(IllegalStateException::class.java) {
            fcmPushSender.send(PushMessage(listOf("t1", "t2", "t3"), "제목", "본문"))
        }
    }

    @Test
    fun `무효 토큰을 뺀 나머지가 모두 일시 오류여도 예외를 던진다`() {
        val fcmPushSender =
            senderReturning(
                0,
                failedResponse(MessagingErrorCode.UNREGISTERED),
                failedResponse(MessagingErrorCode.UNAVAILABLE),
            )

        assertThrows(IllegalStateException::class.java) {
            fcmPushSender.send(PushMessage(listOf("gone", "t2"), "제목", "본문"))
        }
    }

    @Test
    fun `한 명이라도 받았으면 일시 오류가 있어도 중복 발송을 피해 결과만 돌려준다`() {
        val success = mock(SendResponse::class.java)
        `when`(success.isSuccessful).thenReturn(true)
        val fcmPushSender = senderReturning(1, success, failedResponse(MessagingErrorCode.UNAVAILABLE))

        val result = fcmPushSender.send(PushMessage(listOf("ok", "t2"), "제목", "본문"))

        assertEquals(PushSendResult(1, emptyList(), 1), result)
    }

    @Test
    fun `재시도해도 소용없는 실패가 섞여 있으면 예외 없이 결과만 돌려준다`() {
        val fcmPushSender =
            senderReturning(
                0,
                failedResponse(MessagingErrorCode.UNAVAILABLE),
                failedResponse(MessagingErrorCode.INVALID_ARGUMENT),
            )

        val result = fcmPushSender.send(PushMessage(listOf("t1", "t2"), "제목", "본문"))

        assertEquals(PushSendResult(0, emptyList(), 2), result)
    }

    private fun senderReturning(
        successCount: Int,
        vararg responses: SendResponse,
    ): FcmPushSender {
        val firebaseMessaging = mock(FirebaseMessaging::class.java)
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("firebaseMessaging", firebaseMessaging)
        val batchResponse = mock(BatchResponse::class.java)
        `when`(batchResponse.successCount).thenReturn(successCount)
        `when`(batchResponse.responses).thenReturn(responses.toList())
        `when`(firebaseMessaging.sendEachForMulticast(any(MulticastMessage::class.java))).thenReturn(batchResponse)
        return FcmPushSender(
            beanFactory.getBeanProvider(FirebaseMessaging::class.java),
            PushProperties(),
            meterRegistry,
        )
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
