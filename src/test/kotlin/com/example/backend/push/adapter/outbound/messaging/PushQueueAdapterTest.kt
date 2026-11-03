package com.example.backend.push.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.push.application.port.inbound.dto.PushMessage
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import java.util.function.Consumer

/** [PushQueueAdapter] 단위 테스트. 큐 미설정 no-op 과 발행 실패 시 예외를 삼키는지 확인한다. */
class PushQueueAdapterTest {
    private val message = PushMessage(listOf("secret-token"), "제목", "본문")

    @Test
    fun `queueUrl 이 비면 SqsTemplate 을 호출하지 않는다`() {
        val sqsTemplate = mock(SqsTemplate::class.java)

        adapter(sqsTemplate, queueUrl = "").enqueue(message)

        assertEquals(0, sendCount(sqsTemplate))
    }

    @Test
    fun `SqsTemplate 이 없으면 예외 없이 생략한다`() {
        assertDoesNotThrow { adapter(null, QUEUE_URL).enqueue(message) }
    }

    @Test
    fun `queueUrl 과 SqsTemplate 이 있으면 발행한다`() {
        val sqsTemplate = mock(SqsTemplate::class.java)

        adapter(sqsTemplate, QUEUE_URL).enqueue(message)

        assertEquals(1, sendCount(sqsTemplate))
    }

    @Test
    fun `발행이 실패해도 예외를 밖으로 던지지 않는다`() {
        val sqsTemplate = mock(SqsTemplate::class.java)
        Mockito
            .doThrow(IllegalStateException("secret-token 포함 메시지"))
            .`when`(sqsTemplate)
            .send(any<Consumer<io.awspring.cloud.sqs.operations.SqsSendOptions<Any>>>())

        assertDoesNotThrow { adapter(sqsTemplate, QUEUE_URL).enqueue(message) }
    }

    private fun adapter(
        sqsTemplate: SqsTemplate?,
        queueUrl: String,
    ): PushQueueAdapter {
        val beanFactory = DefaultListableBeanFactory()
        sqsTemplate?.let { beanFactory.registerSingleton("sqsTemplate", it) }
        return PushQueueAdapter(
            beanFactory.getBeanProvider(SqsTemplate::class.java),
            SqsProperties(pushQueueUrl = queueUrl),
        )
    }

    // SqsTemplate.send 는 제네릭 오버로드가 많아 verify 대신 호출 기록으로 확인한다.
    private fun sendCount(sqsTemplate: SqsTemplate): Int =
        Mockito.mockingDetails(sqsTemplate).invocations.count { it.method.name == "send" }

    private companion object {
        const val QUEUE_URL = "https://sqs.ap-northeast-2.amazonaws.com/123/push"
    }
}
