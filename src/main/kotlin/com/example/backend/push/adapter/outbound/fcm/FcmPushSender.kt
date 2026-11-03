package com.example.backend.push.adapter.outbound.fcm

import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.outbound.PushSendResult
import com.example.backend.push.application.port.outbound.PushSenderPort
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/** FCM 멀티캐스트 응답을 성공, 무효 토큰, 기타 실패로 분류한다. */
@Component
class FcmPushSender(
    private val firebaseMessagingProvider: ObjectProvider<FirebaseMessaging>,
) : PushSenderPort {
    private val log = KotlinLogging.logger {}

    override fun send(message: PushMessage): PushSendResult {
        val firebaseMessaging = firebaseMessagingProvider.ifAvailable
        if (firebaseMessaging == null) {
            log.warn { "Firebase 미설정으로 발송 생략" }
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
                .build()
        // 호출 전체가 실패하면 예외를 전파해 SQS가 재전송하도록 한다.
        val batchResponse = firebaseMessaging.sendEachForMulticast(multicastMessage)
        val invalidTokens = mutableListOf<String>()
        var failedCount = 0
        batchResponse.responses.forEachIndexed { index, response ->
            if (!response.isSuccessful) {
                if (isInvalidToken(response.exception?.messagingErrorCode)) {
                    invalidTokens.add(message.tokens[index])
                } else {
                    failedCount++
                }
            }
        }
        return PushSendResult(batchResponse.successCount, invalidTokens, failedCount)
    }
}

/**
 * 토큰을 지워도 되는 실패만 무효로 본다. INVALID_ARGUMENT 는 메시지 내용이 잘못돼도 나오므로 넣지 않는다
 * (넣으면 정상 토큰까지 정리될 수 있다).
 */
internal fun isInvalidToken(code: MessagingErrorCode?): Boolean =
    code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.SENDER_ID_MISMATCH
