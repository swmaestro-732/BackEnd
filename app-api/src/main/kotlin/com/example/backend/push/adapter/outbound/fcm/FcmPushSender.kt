package com.example.backend.push.adapter.outbound.fcm

import com.example.backend.push.adapter.config.PushProperties
import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.outbound.PushSendResult
import com.example.backend.push.application.port.outbound.PushSenderPort
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.time.Duration

/** FCM 멀티캐스트 응답을 성공, 무효 토큰, 기타 실패로 분류한다. */
@Component
class FcmPushSender(
    private val firebaseMessagingProvider: ObjectProvider<FirebaseMessaging>,
    private val pushProperties: PushProperties,
    meterRegistry: MeterRegistry,
) : PushSenderPort {
    private val log = KotlinLogging.logger {}
    private val firebaseNotConfiguredCounter =
        meterRegistry.counter("push.skipped", "reason", "firebase_not_configured")

    override fun send(message: PushMessage): PushSendResult {
        val firebaseMessaging = firebaseMessagingProvider.ifAvailable
        if (firebaseMessaging == null) {
            firebaseNotConfiguredCounter.increment(message.tokens.size.toDouble())
            log.warn { "Firebase 미설정으로 발송 생략: tokenCount=${message.tokens.size}" }
            return PushSendResult(0, emptyList(), 0)
        }
        val multicastMessage =
            MulticastMessage
                .builder()
                .addAllTokens(message.tokens)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(message.title)
                        .setBody(message.body)
                        .build(),
                ).putAllData(message.data)
                .setAndroidConfig(androidConfig(pushProperties.androidTtl, pushProperties.androidChannelId))
                .build()
        // 토큰별 실패는 예외 없이 응답에 담겨 온다. 호출 자체의 예외만 그대로 전파된다.
        val batchResponse = firebaseMessaging.sendEachForMulticast(multicastMessage)
        val invalidTokens = mutableListOf<String>()
        var failedCount = 0
        var allFailuresRetryable = true
        batchResponse.responses.forEachIndexed { index, response ->
            if (!response.isSuccessful) {
                val code = response.exception?.messagingErrorCode
                if (isInvalidToken(code)) {
                    invalidTokens.add(message.tokens[index])
                } else {
                    failedCount++
                    if (!isRetryable(code)) allFailuresRetryable = false
                }
            }
        }
        // FCM 장애로 아무도 받지 못했으면 예외로 SQS 재전송과 DLQ 에 맡긴다. 한 명이라도 받았으면 중복 발송을 피해 넘긴다.
        if (batchResponse.successCount == 0 && failedCount > 0 && allFailuresRetryable) {
            throw IllegalStateException("FCM 일시 오류로 묶음 전체 발송 실패: failedCount=$failedCount")
        }
        return PushSendResult(batchResponse.successCount, invalidTokens, failedCount)
    }
}

/** 오프라인 기기 보관 기간과 앱의 알림 채널을 지정한다. 채널이 없으면 안드로이드가 기본 채널로 보여 사용자가 끌 수 없다. */
internal fun androidConfig(
    ttl: Duration,
    channelId: String,
): AndroidConfig =
    AndroidConfig
        .builder()
        .setTtl(ttl.toMillis())
        .setNotification(AndroidNotification.builder().setChannelId(channelId).build())
        .build()

/**
 * 토큰을 지워도 되는 실패만 무효로 본다. INVALID_ARGUMENT 는 메시지 내용이 잘못돼도 나오므로 넣지 않는다
 * (넣으면 정상 토큰까지 정리될 수 있다). SENDER_ID_MISMATCH 는 서버 키와 토큰의 Firebase 프로젝트가 다른
 * 설정 문제라 넣지 않는다(키를 잘못 넣으면 모든 토큰이 지워진다).
 */
internal fun isInvalidToken(code: MessagingErrorCode?): Boolean = code == MessagingErrorCode.UNREGISTERED

/** 다시 보내면 성공할 수 있는 일시 오류. 코드가 없으면 네트워크 오류로 본다. */
internal fun isRetryable(code: MessagingErrorCode?): Boolean =
    code == null ||
        code == MessagingErrorCode.UNAVAILABLE ||
        code == MessagingErrorCode.INTERNAL ||
        code == MessagingErrorCode.QUOTA_EXCEEDED
