package com.example.backend.push.adapter.inbound.messaging

import com.example.backend.push.adapter.messaging.PushQueueMessage
import com.example.backend.push.application.port.inbound.PushUseCase
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

/**
 * 큐와 Firebase 키가 모두 설정된 환경에서만 수신한다. 키 없이 수신하면 발송은 생략되는데 메시지는 ack 되어 사라지므로,
 * 그때는 리스너를 띄우지 않고 메시지를 큐에 남긴다. 예외를 전파해 SQS 재전송과 DLQ 처리를 인프라에 맡긴다.
 */
@Component
@ConditionalOnExpression(
    // 키 JSON 은 따옴표를 포함할 수 있어 placeholder 로 넣지 않고 FirebaseConfig 와 같이 environment 로 읽는다.
    "'\${aws.sqs.push-queue-url:}'.trim().length() > 0 && " +
        "@environment.getProperty('firebase.credentials-json', '').trim().length() > 0",
)
class PushSqsConsumer(
    private val pushUseCase: PushUseCase,
) {
    @SqsListener("\${aws.sqs.push-queue-url}")
    fun onMessage(message: PushQueueMessage) {
        pushUseCase.deliver(message.toPushMessage())
    }
}
