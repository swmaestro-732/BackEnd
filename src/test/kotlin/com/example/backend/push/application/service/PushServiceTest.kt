package com.example.backend.push.application.service

import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.outbound.PushQueuePort
import com.example.backend.push.application.port.outbound.PushSendResult
import com.example.backend.push.application.port.outbound.PushSenderPort
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PushServiceTest {
    private val enqueuedMessages = mutableListOf<PushMessage>()
    private val sentMessages = mutableListOf<PushMessage>()
    private val sendResults = ArrayDeque<PushSendResult>()
    private var sendFailure: RuntimeException? = null
    private val meterRegistry = SimpleMeterRegistry()

    private val pushQueuePort =
        object : PushQueuePort {
            override fun enqueue(message: PushMessage) {
                enqueuedMessages.add(message)
            }
        }

    private val pushSenderPort =
        object : PushSenderPort {
            override fun send(message: PushMessage): PushSendResult {
                sendFailure?.let { throw it }
                sentMessages.add(message)
                return sendResults.removeFirstOrNull() ?: PushSendResult(message.tokens.size, emptyList(), 0)
            }
        }

    private val pushService = PushService(pushQueuePort, pushSenderPort, meterRegistry)
    private val message = PushMessage(listOf("token-1"), "제목", "본문", mapOf("type" to "test"))

    @Test
    fun `토큰이 없으면 큐에 발행하지 않는다`() {
        pushService.request(message.copy(tokens = emptyList()))

        assertTrue(enqueuedMessages.isEmpty())
        assertTrue(sentMessages.isEmpty())
    }

    @Test
    fun `토큰이 있으면 메시지를 큐에 발행한다`() {
        pushService.request(message)

        assertEquals(listOf(message), enqueuedMessages)
        assertTrue(sentMessages.isEmpty())
    }

    @Test
    fun `토큰 1001개는 500개씩 나누어 큐 메시지 3개로 발행한다`() {
        val tokens = (1..1001).map { "token-$it" }

        pushService.request(message.copy(tokens = tokens))

        assertEquals(listOf(500, 500, 1), enqueuedMessages.map { it.tokens.size })
        assertEquals(tokens, enqueuedMessages.flatMap { it.tokens })
        enqueuedMessages.forEach { assertEquals("제목", it.title) }
    }

    @Test
    fun `발송 전에 모든 결과 카운터가 0으로 등록된다`() {
        for (result in listOf("success", "invalid_token", "failed")) {
            assertEquals(
                0.0,
                meterRegistry
                    .get("push.sent")
                    .tag("result", result)
                    .counter()
                    .count(),
            )
        }
    }

    @Test
    fun `토큰 1001개를 500개씩 나누어 메시지 내용을 유지하고 결과를 합산한다`() {
        val tokens = (1..1001).map { "token-$it" }
        sendResults.addAll(
            listOf(
                PushSendResult(490, tokens.subList(490, 495), 5),
                PushSendResult(497, listOf(tokens[997]), 2),
                PushSendResult(1, emptyList(), 0),
            ),
        )

        pushService.deliver(message.copy(tokens = tokens))

        assertEquals(listOf(500, 500, 1), sentMessages.map { it.tokens.size })
        assertEquals(tokens, sentMessages.flatMap { it.tokens })
        sentMessages.forEach {
            assertEquals("제목", it.title)
            assertEquals("본문", it.body)
            assertEquals(mapOf("type" to "test"), it.data)
        }
        assertEquals(
            988.0,
            meterRegistry
                .get("push.sent")
                .tag("result", "success")
                .counter()
                .count(),
        )
        assertEquals(
            6.0,
            meterRegistry
                .get("push.sent")
                .tag("result", "invalid_token")
                .counter()
                .count(),
        )
        assertEquals(
            7.0,
            meterRegistry
                .get("push.sent")
                .tag("result", "failed")
                .counter()
                .count(),
        )
    }

    @Test
    fun `빈 토큰은 발송하지 않고 카운터도 증가하지 않는다`() {
        pushService.deliver(message.copy(tokens = emptyList()))

        assertTrue(sentMessages.isEmpty())
        for (result in listOf("success", "invalid_token", "failed")) {
            assertEquals(
                0.0,
                meterRegistry
                    .get("push.sent")
                    .tag("result", result)
                    .counter()
                    .count(),
            )
        }
    }

    @Test
    fun `발송 예외는 재시도를 위해 전파한다`() {
        val failure = IllegalStateException("발송 실패")
        sendFailure = failure

        val thrown = assertThrows(IllegalStateException::class.java) { pushService.deliver(message) }

        assertSame(failure, thrown)
        assertEquals(
            0.0,
            meterRegistry
                .get("push.sent")
                .tag("result", "success")
                .counter()
                .count(),
        )
    }
}
