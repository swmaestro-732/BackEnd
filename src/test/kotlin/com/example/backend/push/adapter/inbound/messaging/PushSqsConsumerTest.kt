package com.example.backend.push.adapter.inbound.messaging

import com.example.backend.bootstrap.config.PushProperties
import com.example.backend.push.adapter.messaging.PushQueueMessage
import com.example.backend.push.application.port.inbound.PushDeliveryUseCase
import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.google.firebase.messaging.FirebaseMessagingException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class PushSqsConsumerTest {
    private val deliveredMessages = mutableListOf<PushMessage>()
    private var deliveryFailure: Exception? = null
    private val pushDeliveryUseCase =
        object : PushDeliveryUseCase {
            override fun deliver(message: PushMessage) {
                // 실제 FcmPushSender 처럼 checked 예외(FirebaseMessagingException)도 그대로 던진다.
                deliveryFailure?.let { throw it }
                deliveredMessages.add(message)
            }
        }
    private val now = Instant.parse("2026-11-04T01:00:00Z")
    private val meterRegistry = SimpleMeterRegistry()
    private val pushSqsConsumer =
        PushSqsConsumer(
            pushDeliveryUseCase,
            PushProperties(maxAge = Duration.ofHours(1)),
            Clock.fixed(now, ZoneOffset.UTC),
            meterRegistry,
        )
    private val message = PushMessage(listOf("token-1", "token-2"), "제목", "본문", mapOf("type" to "test"))

    @Test
    fun `큐 메시지를 발송 요청으로 변환해 전달한다`() {
        pushSqsConsumer.onMessage(PushQueueMessage(message, now.minusSeconds(60)))

        assertEquals(listOf(message), deliveredMessages)
    }

    @Test
    fun `생성 후 최대 보관 시간 정각인 메시지는 발송한다`() {
        pushSqsConsumer.onMessage(PushQueueMessage(message, now.minus(Duration.ofHours(1))))

        assertEquals(listOf(message), deliveredMessages)
        assertEquals(0.0, skippedCount("stale"))
    }

    @Test
    fun `최대 보관 시간을 넘긴 메시지는 발송하지 않고 토큰 수만큼 stale 로 센다`() {
        pushSqsConsumer.onMessage(PushQueueMessage(message, now.minus(Duration.ofHours(1)).minusSeconds(1)))

        assertTrue(deliveredMessages.isEmpty())
        assertEquals(2.0, skippedCount("stale"))
    }

    @Test
    fun `TTL 30분 메시지는 최대 보관 시간 안이어도 31분이 지나면 stale 로 버린다`() {
        val marketing = message.copy(androidTtl = Duration.ofMinutes(30))

        pushSqsConsumer.onMessage(PushQueueMessage(marketing, now.minus(Duration.ofMinutes(31))))

        assertTrue(deliveredMessages.isEmpty())
        assertEquals(2.0, skippedCount("stale"))
    }

    @Test
    fun `TTL 메시지는 큐에서 기다린 만큼 줄인 TTL 로 발송해 큐 대기와 기기 보관 합이 TTL 을 넘지 않게 한다`() {
        val marketing = message.copy(androidTtl = Duration.ofMinutes(30))

        pushSqsConsumer.onMessage(PushQueueMessage(marketing, now.minus(Duration.ofMinutes(29))))

        assertEquals(Duration.ofMinutes(1), deliveredMessages.single().androidTtl)
    }

    @Test
    fun `TTL 메시지가 TTL 정각만큼 기다렸으면 버리지 않고 TTL 0 으로 발송한다`() {
        val marketing = message.copy(androidTtl = Duration.ofMinutes(30))

        pushSqsConsumer.onMessage(PushQueueMessage(marketing, now.minus(Duration.ofMinutes(30))))

        assertEquals(Duration.ZERO, deliveredMessages.single().androidTtl)
        assertEquals(0.0, skippedCount("stale"))
    }

    @Test
    fun `TTL 메시지가 TTL 보다 1초 더 기다렸으면 stale 로 버린다`() {
        val marketing = message.copy(androidTtl = Duration.ofMinutes(30))

        pushSqsConsumer.onMessage(PushQueueMessage(marketing, now.minus(Duration.ofMinutes(30)).minusSeconds(1)))

        assertTrue(deliveredMessages.isEmpty())
        assertEquals(2.0, skippedCount("stale"))
    }

    @Test
    fun `생성 시각이 소비자 시계보다 앞서도 TTL 을 늘리지 않고 그대로 발송한다`() {
        val marketing = message.copy(androidTtl = Duration.ofMinutes(30))

        pushSqsConsumer.onMessage(PushQueueMessage(marketing, now.plusSeconds(5)))

        assertEquals(Duration.ofMinutes(30), deliveredMessages.single().androidTtl)
    }

    @Test
    fun `TTL 이 없는 메시지는 큐에서 기다려도 TTL 없이 발송해 설정값을 쓰게 한다`() {
        pushSqsConsumer.onMessage(PushQueueMessage(message, now.minus(Duration.ofMinutes(29))))

        assertNull(deliveredMessages.single().androidTtl)
    }

    @Test
    fun `생성 시각이 없는 이전 형식 메시지는 오래되지 않은 것으로 보고 발송한다`() {
        pushSqsConsumer.onMessage(PushQueueMessage(message.tokens, message.title, message.body, message.data))

        assertEquals(listOf(message), deliveredMessages)
    }

    @Test
    fun `발송 중 IllegalArgumentException 은 메시지를 버리지 않고 재전송되도록 전파한다`() {
        val failure = IllegalArgumentException("설정 오류")
        deliveryFailure = failure

        val thrown =
            assertThrows(
                IllegalArgumentException::class.java,
            ) { pushSqsConsumer.onMessage(PushQueueMessage(message, now)) }

        assertSame(failure, thrown)
        assertEquals(0.0, skippedCount("invalid_message"))
    }

    @Test
    fun `검증을 통과하지 못하는 큐 메시지도 정상 반환하고 invalid_message 로 센다`() {
        val queueMessage = PushQueueMessage(listOf("token-1"), "제목", "본문", mapOf("from" to "x"), now)

        assertDoesNotThrow { pushSqsConsumer.onMessage(queueMessage) }

        assertTrue(deliveredMessages.isEmpty())
        assertEquals(1.0, skippedCount("invalid_message"))
    }

    @Test
    fun `FirebaseMessagingException 은 SQS 가 재전송하도록 전파한다`() {
        val failure = mock(FirebaseMessagingException::class.java)
        deliveryFailure = failure

        val thrown =
            assertThrows(FirebaseMessagingException::class.java) {
                pushSqsConsumer.onMessage(PushQueueMessage(message, now))
            }

        assertSame(failure, thrown)
        assertEquals(0.0, skippedCount("invalid_message"))
    }

    @Test
    fun `그 밖의 발송 실패도 SQS 가 재전송하도록 전파한다`() {
        val failure = IllegalStateException("발송 실패")
        deliveryFailure = failure

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                pushSqsConsumer.onMessage(PushQueueMessage(message, now))
            }

        assertSame(failure, thrown)
    }

    private fun skippedCount(reason: String): Double =
        meterRegistry
            .get("push.skipped")
            .tag("reason", reason)
            .counter()
            .count()
}
