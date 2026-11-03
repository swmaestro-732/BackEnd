package com.example.backend.push.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.push.adapter.messaging.PushQueueMessage
import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.outbound.PushQueuePort
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/** SQS에 푸시 발송 요청을 발행한다. 큐 미설정 시 생략하고 발행 실패는 오류 로그로 남긴다. */
@Component
class PushQueueAdapter(
    private val sqsTemplateProvider: ObjectProvider<SqsTemplate>,
    private val sqsProperties: SqsProperties,
) : PushQueuePort {
    private val log = KotlinLogging.logger {}

    override fun enqueue(message: PushMessage) {
        val queueUrl = sqsProperties.pushQueueUrl
        if (queueUrl.isBlank()) {
            log.warn { "SQS queueUrl 미설정으로 푸시 발행 생략" }
            return
        }
        val template = sqsTemplateProvider.ifAvailable ?: return // SQS 미설정 시 생략한다.
        try {
            template.send { to -> to.queue(queueUrl).payload(PushQueueMessage(message)) }
        } catch (e: Exception) {
            // 예외 메시지에 토큰이 포함될 수 있어 오류 종류만 기록한다.
            log.error { "푸시 큐 발행 실패(발송 유실 가능): error=${e.javaClass.simpleName}" }
        }
    }
}
