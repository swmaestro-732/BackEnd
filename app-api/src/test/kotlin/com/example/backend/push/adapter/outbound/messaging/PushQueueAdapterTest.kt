package com.example.backend.push.adapter.outbound.messaging

import com.example.backend.push.adapter.config.PushQueueProperties
import com.example.backend.push.adapter.messaging.PushQueueMessage
import com.example.backend.push.application.port.inbound.dto.PushMessage
import io.awspring.cloud.sqs.operations.SqsSendOptions
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.function.Consumer

/** [PushQueueAdapter] 단위 테스트. 큐 미설정 no-op 과 발행 실패 시 예외를 삼키는지 확인한다. */
class PushQueueAdapterTest {
    private val message = PushMessage(listOf("secret-token", "token-2"), "제목", "본문")
    private val now = Instant.parse("2026-11-04T01:02:03Z")
    private val meterRegistry = SimpleMeterRegistry()

    @Test
    fun `queueUrl 이 비면 SqsTemplate 을 호출하지 않고 토큰 수만큼 생략 카운터를 올린다`() {
        val sqsTemplate = mock(SqsTemplate::class.java)

        adapter(sqsTemplate, queueUrl = "").enqueue(message)

        assertEquals(0, sendCount(sqsTemplate))
        assertEquals(2.0, queueNotConfiguredCount())
    }

    @Test
    fun `SqsTemplate 이 없으면 예외 없이 생략하고 생략 카운터를 올린다`() {
        assertDoesNotThrow { adapter(null, QUEUE_URL).enqueue(message) }

        assertEquals(2.0, queueNotConfiguredCount())
    }

    @Test
    fun `queueUrl 과 SqsTemplate 이 있으면 주입된 시각을 담아 발행한다`() {
        val sqsTemplate = mock(SqsTemplate::class.java)

        adapter(sqsTemplate, QUEUE_URL).enqueue(message)

        assertEquals(1, sendCount(sqsTemplate))
        assertEquals(PushQueueMessage(message, now), sentPayload(sqsTemplate))
        assertEquals(0.0, queueNotConfiguredCount())
    }

    @Test
    fun `발행이 실패해도 예외를 밖으로 던지지 않는다`() {
        val sqsTemplate = mock(SqsTemplate::class.java)
        Mockito
            .doThrow(IllegalStateException("secret-token 포함 메시지"))
            .`when`(sqsTemplate)
            .send(any<Consumer<SqsSendOptions<Any>>>())

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
            PushQueueProperties(pushQueueUrl = queueUrl),
            Clock.fixed(now, ZoneOffset.UTC),
            meterRegistry,
        )
    }

    private fun queueNotConfiguredCount(): Double =
        meterRegistry
            .get("push.skipped")
            .tag("reason", "queue_not_configured")
            .counter()
            .count()

    // SqsTemplate.send 는 제네릭 오버로드가 많아 verify 대신 호출 기록으로 확인한다.
    private fun sendCount(sqsTemplate: SqsTemplate): Int =
        Mockito.mockingDetails(sqsTemplate).invocations.count { it.method.name == "send" }

    // 발행 시 넘긴 옵션 람다를 빌더 목에 적용해 payload 를 꺼낸다.
    @Suppress("UNCHECKED_CAST")
    private fun sentPayload(sqsTemplate: SqsTemplate): Any? {
        val options = mock(SqsSendOptions::class.java, Answers.RETURNS_SELF) as SqsSendOptions<Any>
        val invocation = Mockito.mockingDetails(sqsTemplate).invocations.single { it.method.name == "send" }
        (invocation.arguments.single() as Consumer<SqsSendOptions<Any>>).accept(options)
        val payloadCall = Mockito.mockingDetails(options).invocations.single { it.method.name == "payload" }
        return payloadCall.arguments.single()
    }

    private companion object {
        const val QUEUE_URL = "https://sqs.ap-northeast-2.amazonaws.com/123/push"
    }
}
