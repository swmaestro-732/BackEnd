package com.example.backend.push.application.service

import com.example.backend.push.application.port.inbound.PushDeliveryUseCase
import com.example.backend.push.application.port.inbound.PushUseCase
import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.outbound.PushQueuePort
import com.example.backend.push.application.port.outbound.PushSenderPort
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

/** 큐 발행과 FCM 한도에 맞춘 분할 발송을 처리한다. */
@Service
class PushService(
    private val pushQueuePort: PushQueuePort,
    private val pushSenderPort: PushSenderPort,
    meterRegistry: MeterRegistry,
) : PushUseCase,
    PushDeliveryUseCase {
    private val log = KotlinLogging.logger {}

    // 첫 발송도 Prometheus increase()에 잡히도록 생성 시 0으로 등록한다.
    private val successCounter = meterRegistry.counter("push.sent", "result", "success")
    private val invalidTokenCounter = meterRegistry.counter("push.sent", "result", "invalid_token")
    private val failedCounter = meterRegistry.counter("push.sent", "result", "failed")

    init {
        // 발송하지 못한 사유도 첫 발생부터 보이도록 0으로 등록한다. 증가는 사유가 생기는 각 어댑터가 한다.
        for (reason in listOf("firebase_not_configured", "queue_not_configured", "stale", "invalid_message")) {
            meterRegistry.counter("push.skipped", "reason", reason)
        }
    }

    // 큐 메시지 하나에 FCM 한도(500)까지만 담는다. 발송 중 예외로 재전송되어도 다시 받는 범위가 그 묶음으로 그친다.
    override fun request(message: PushMessage) {
        for (tokens in message.tokens.chunked(FCM_MULTICAST_LIMIT)) {
            pushQueuePort.enqueue(message.copy(tokens = tokens))
        }
    }

    override fun deliver(message: PushMessage) {
        var invalidTokenCount = 0
        for (tokens in message.tokens.chunked(FCM_MULTICAST_LIMIT)) {
            val result = pushSenderPort.send(message.copy(tokens = tokens))
            successCounter.increment(result.successCount.toDouble())
            invalidTokenCounter.increment(result.invalidTokens.size.toDouble())
            failedCounter.increment(result.failedCount.toDouble())
            invalidTokenCount += result.invalidTokens.size
        }
        // 무효 토큰 정리는 devices 테이블 반영 후 연결한다(SCRUM-765 후속).
        if (invalidTokenCount > 0) {
            log.warn { "푸시 발송 무효 토큰: count=$invalidTokenCount" }
        }
    }

    private companion object {
        const val FCM_MULTICAST_LIMIT = 500
    }
}
