package com.example.backend.fcm

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * FCM 멀티캐스트 발송. [FirebaseMessaging] 빈은 서비스 계정 JSON 이 있을 때만 생기므로([FirebaseConfig]) [ObjectProvider] 로 받는다.
 * 토큰별 결과 해석(무효 토큰, 재시도 판단)은 호출하는 쪽이 한다.
 */
@Component
class FcmSender(
    private val firebaseMessagingProvider: ObjectProvider<FirebaseMessaging>,
) {
    /**
     * Firebase 미설정이면 보내지 않고 null. 토큰별 실패는 예외 없이 결과에 담기고, 호출 자체의 예외만 그대로 전파된다.
     * 결과의 [FcmBatchResult.results] 는 [tokens] 와 같은 순서다.
     */
    fun sendMulticast(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>,
        androidTtl: Duration,
        androidChannelId: String,
    ): FcmBatchResult? {
        val firebaseMessaging = firebaseMessagingProvider.ifAvailable ?: return null
        val multicastMessage =
            MulticastMessage
                .builder()
                .addAllTokens(tokens)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(title)
                        .setBody(body)
                        .build(),
                ).putAllData(data)
                .setAndroidConfig(androidConfig(androidTtl, androidChannelId))
                .build()
        val batchResponse = firebaseMessaging.sendEachForMulticast(multicastMessage)
        return FcmBatchResult(
            successCount = batchResponse.successCount,
            results =
                batchResponse.responses.map {
                    FcmTokenResult(successful = it.isSuccessful, errorCode = it.exception?.messagingErrorCode)
                },
        )
    }
}

/** 멀티캐스트 발송 결과. [results] 는 요청 토큰과 같은 순서다. */
data class FcmBatchResult(
    val successCount: Int,
    val results: List<FcmTokenResult>,
)

/** 토큰 하나의 발송 결과. 실패면 FCM 오류 코드(없으면 네트워크 오류 등)를 담는다. */
data class FcmTokenResult(
    val successful: Boolean,
    val errorCode: MessagingErrorCode?,
)

/** 오프라인 기기 보관 기간과 앱의 알림 채널을 지정한다. 채널이 없으면 안드로이드가 기본 채널로 보여 사용자가 끌 수 없다. */
fun androidConfig(
    ttl: Duration,
    channelId: String,
): AndroidConfig =
    AndroidConfig
        .builder()
        .setTtl(ttl.toMillis())
        .setNotification(AndroidNotification.builder().setChannelId(channelId).build())
        .build()
