package com.example.backend.sqs

import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * SQS 범용 발행. [SqsTemplate] 빈은 spring-cloud-aws 가 `spring.cloud.aws.*` 로 자동 배선하고,
 * SQS 미설정(로컬, CI)이면 없을 수 있어 [ObjectProvider] 로 받는다.
 * 큐 URL 선택, 미설정 시 로그, 발행 실패 처리는 메시지마다 달라 호출하는 어댑터가 한다.
 */
@Component
class SqsMessageSender(
    private val sqsTemplateProvider: ObjectProvider<SqsTemplate>,
) {
    /** SqsTemplate 빈이 있는지. 없으면 발행할 수 없다. */
    val isAvailable: Boolean
        get() = sqsTemplateProvider.ifAvailable != null

    /** [queueUrl] 로 [payload] 를 발행한다. SqsTemplate 이 없으면 아무것도 하지 않는다. 발행 실패는 예외로 던진다. */
    fun send(
        queueUrl: String,
        payload: Any,
    ) {
        val template = sqsTemplateProvider.ifAvailable ?: return
        template.send { to -> to.queue(queueUrl).payload(payload) }
    }
}
