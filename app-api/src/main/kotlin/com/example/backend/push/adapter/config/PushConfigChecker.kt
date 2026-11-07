package com.example.backend.push.adapter.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/** 푸시 큐와 Firebase 키 중 하나만 설정된 배포를 기동 시 오류 로그로 알린다. 키 값은 남기지 않는다. */
@Component
class PushConfigChecker(
    private val pushQueueProperties: PushQueueProperties,
    private val environment: Environment,
) {
    private val log = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    fun check() {
        // 키 JSON 은 따옴표를 포함할 수 있어 FirebaseConfig 와 같이 environment 로 읽는다.
        val credentialsJson = environment.getProperty("firebase.credentials-json", "")
        pushConfigMismatchMessage(pushQueueProperties.pushQueueUrl, credentialsJson)?.let { log.error { it } }
    }
}

/** 둘 다 있거나 둘 다 없으면(로컬/CI) 정상이라 null 을 반환한다. */
internal fun pushConfigMismatchMessage(
    pushQueueUrl: String,
    credentialsJson: String,
): String? {
    val hasQueue = pushQueueUrl.isNotBlank()
    val hasKey = credentialsJson.isNotBlank()
    return when {
        hasQueue && !hasKey -> {
            "푸시 큐는 설정됐지만 Firebase 키(FIREBASE_CREDENTIALS_JSON)가 비어 있어 수신 리스너를 띄우지 않습니다. " +
                "푸시가 발송되지 않고 큐에 쌓입니다."
        }

        !hasQueue && hasKey -> {
            "Firebase 키는 설정됐지만 푸시 큐(SQS_PUSH_QUEUE_URL)가 비어 있어 발행을 생략합니다. 푸시가 발송되지 않습니다."
        }

        else -> {
            null
        }
    }
}
