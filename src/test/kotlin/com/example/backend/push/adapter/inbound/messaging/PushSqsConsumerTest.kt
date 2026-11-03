package com.example.backend.push.adapter.inbound.messaging

import com.example.backend.push.adapter.messaging.PushQueueMessage
import com.example.backend.push.application.port.inbound.PushUseCase
import com.example.backend.push.application.port.inbound.dto.PushMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PushSqsConsumerTest {
    private val deliveredMessages = mutableListOf<PushMessage>()
    private var deliveryFailure: RuntimeException? = null
    private val pushUseCase =
        object : PushUseCase {
            override fun request(message: PushMessage) = error("수신은 큐 발행을 호출하지 않는다")

            override fun deliver(message: PushMessage) {
                deliveryFailure?.let { throw it }
                deliveredMessages.add(message)
            }
        }
    private val pushSqsConsumer = PushSqsConsumer(pushUseCase)
    private val message = PushMessage(listOf("token-1"), "제목", "본문", mapOf("type" to "test"))

    @Test
    fun `큐 메시지를 발송 요청으로 변환해 전달한다`() {
        pushSqsConsumer.onMessage(PushQueueMessage(message))

        assertEquals(listOf(message), deliveredMessages)
    }

    @Test
    fun `발송 실패는 SQS가 재전송하도록 전파한다`() {
        val failure = IllegalStateException("발송 실패")
        deliveryFailure = failure

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                pushSqsConsumer.onMessage(PushQueueMessage(message))
            }

        assertSame(failure, thrown)
    }
}
