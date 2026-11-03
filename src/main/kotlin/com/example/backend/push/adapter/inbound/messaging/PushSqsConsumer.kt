package com.example.backend.push.adapter.inbound.messaging

import com.example.backend.bootstrap.config.PushProperties
import com.example.backend.push.adapter.messaging.PushQueueMessage
import com.example.backend.push.application.port.inbound.PushDeliveryUseCase
import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

/**
 * 큐와 Firebase 키가 모두 설정된 환경에서만 수신한다. 키 없이 수신하면 발송은 생략되는데 메시지는 ack 되어 사라지므로,
 * 그때는 리스너를 띄우지 않고 메시지를 큐에 남긴다. 예외를 전파해 SQS 재전송과 DLQ 처리를 인프라에 맡긴다.
 * 단 오래된 메시지와 내용이 잘못된 메시지는 재시도해도 소용없으므로 정상 반환(ack)해 버린다.
 */
@Component
@ConditionalOnExpression(
    // 키 JSON 은 따옴표를 포함할 수 있어 placeholder 로 넣지 않고 FirebaseConfig 와 같이 environment 로 읽는다.
    "'\${aws.sqs.push-queue-url:}'.trim().length() > 0 && " +
        "@environment.getProperty('firebase.credentials-json', '').trim().length() > 0",
)
class PushSqsConsumer(
    private val pushDeliveryUseCase: PushDeliveryUseCase,
    private val pushProperties: PushProperties,
    private val clock: Clock,
    meterRegistry: MeterRegistry,
) {
    private val log = KotlinLogging.logger {}
    private val staleCounter = meterRegistry.counter("push.skipped", "reason", "stale")
    private val invalidMessageCounter = meterRegistry.counter("push.skipped", "reason", "invalid_message")

    @SqsListener("\${aws.sqs.push-queue-url}")
    fun onMessage(message: PushQueueMessage) {
        val createdAt = message.createdAt
        if (createdAt != null && Duration.between(createdAt, clock.instant()) > pushProperties.maxAge) {
            staleCounter.increment(message.tokens.size.toDouble())
            log.warn { "오래된 푸시 메시지라 발송하지 않고 버림: tokenCount=${message.tokens.size}, createdAt=$createdAt" }
            return
        }
        try {
            pushDeliveryUseCase.deliver(message.toPushMessage())
        } catch (e: IllegalArgumentException) {
            // 같은 메시지는 몇 번을 다시 받아도 실패하므로 재시도하지 않는다. 예외 메시지는 키 이름 수준이라 토큰은 남지 않는다.
            invalidMessageCounter.increment(message.tokens.size.toDouble())
            log.error { "잘못된 푸시 메시지라 발송하지 않고 버림: tokenCount=${message.tokens.size}, error=${e.message}" }
        }
    }
}
