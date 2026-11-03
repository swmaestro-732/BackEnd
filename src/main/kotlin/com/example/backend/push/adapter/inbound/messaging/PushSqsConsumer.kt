package com.example.backend.push.adapter.inbound.messaging

import com.example.backend.push.adapter.messaging.PushQueueMessage
import com.example.backend.push.application.port.inbound.PushUseCase
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

/** 큐가 설정된 환경에서만 수신한다. 예외를 전파해 SQS 재전송과 DLQ 처리를 인프라에 맡긴다. */
@Component
@ConditionalOnExpression("'\${aws.sqs.push-queue-url:}'.trim().length() > 0")
class PushSqsConsumer(
    private val pushUseCase: PushUseCase,
) {
    @SqsListener("\${aws.sqs.push-queue-url}")
    fun onMessage(message: PushQueueMessage) {
        pushUseCase.deliver(message.toPushMessage())
    }
}
