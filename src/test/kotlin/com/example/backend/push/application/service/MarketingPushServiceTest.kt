package com.example.backend.push.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.PushErrorCode
import com.example.backend.push.application.port.inbound.PushUseCase
import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.inbound.dto.SendMarketingPushCommand
import com.example.backend.push.application.port.outbound.MarketingRecipientPort
import com.example.backend.push.application.port.outbound.PushCampaign
import com.example.backend.push.application.port.outbound.PushCampaignPersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MarketingPushServiceTest {
    private var tokens = listOf("token-1", "token-2")
    private var enqueuedCount: Int? = null
    private val requestedNights = mutableListOf<Boolean>()
    private val savedCampaigns = mutableListOf<Pair<PushCampaign, Instant>>()
    private val enqueuedCounts = mutableMapOf<Long, Int>()
    private val recipientCounts = mutableMapOf<Long, Int>()
    private val requestedMessages = mutableListOf<PushMessage>()
    private var now = kst("2026-11-04T12:00")

    private val marketingRecipientPort =
        object : MarketingRecipientPort {
            override fun findTokens(night: Boolean): List<String> {
                requestedNights.add(night)
                return tokens
            }
        }

    // 실제 어댑터처럼 저장 시각을 남겨 최근 발송 여부를 판단한다.
    private val pushCampaignPersistencePort =
        object : PushCampaignPersistencePort {
            override fun saveUnlessRecent(
                campaign: PushCampaign,
                since: Instant,
            ): Long? {
                val duplicated =
                    savedCampaigns.any { (saved, createdAt) ->
                        saved.adminEmail == campaign.adminEmail && saved.title == campaign.title &&
                            saved.body == campaign.body && createdAt >= since
                    }
                if (duplicated) return null
                savedCampaigns.add(campaign to now)
                return savedCampaigns.size.toLong()
            }

            override fun updateCounts(
                id: Long,
                recipientCount: Int,
                enqueuedCount: Int,
            ) {
                recipientCounts[id] = recipientCount
                enqueuedCounts[id] = enqueuedCount
            }
        }

    private val pushUseCase =
        object : PushUseCase {
            override fun request(message: PushMessage): Int {
                requestedMessages.add(message)
                return enqueuedCount ?: message.tokens.size
            }
        }

    private fun send(sendCommand: SendMarketingPushCommand = command) =
        MarketingPushService(
            marketingRecipientPort,
            pushCampaignPersistencePort,
            pushUseCase,
            Clock.fixed(now, ZoneOffset.UTC),
        ).send(sendCommand)

    private val command = SendMarketingPushCommand("admin@example.com", "가을 코스", "단풍 코스를 만나보세요", LINK)

    @ParameterizedTest
    @CsvSource("2026-11-04T20:29, false", "2026-11-04T21:00, true", "2026-11-05T07:59, true", "2026-11-05T08:00, false")
    fun `KST 21시부터 8시 전까지는 야간 동의 기기만 대상으로 조회한다`(
        time: String,
        night: Boolean,
    ) {
        now = kst(time)

        send()

        assertEquals(listOf(night), requestedNights)
        assertEquals(night, savedCampaigns.single().first.night)
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-11-04T20:30", "2026-11-04T20:59"])
    fun `KST 20시 30분부터 21시 전까지는 발송을 거부하고 기록하지 않는다`(time: String) {
        now = kst(time)

        val exception = assertThrows(BusinessException::class.java) { send() }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        assertEquals("20시 30분부터 21시까지는 광고 발송을 할 수 없습니다", exception.message)
        assertTrue(requestedNights.isEmpty())
        assertTrue(savedCampaigns.isEmpty())
    }

    @Test
    fun `제목에 서비스명을, 본문에 광고 표기와 수신거부 안내를 붙이고 TTL 30분으로 발송을 요청한다`() {
        val result = send()

        val message = requestedMessages.single()
        assertEquals(listOf("token-1", "token-2"), message.tokens)
        assertEquals("[커미] 가을 코스", message.title)
        assertEquals("(광고) 단풍 코스를 만나보세요\n수신거부: 마이 > 설정 > 알림", message.body)
        assertEquals(mapOf("type" to "MARKETING", "link" to LINK), message.data)
        assertEquals(Duration.ofMinutes(30), message.androidTtl)
        assertEquals(
            PushCampaign("admin@example.com", message.title, message.body, LINK, false, 0),
            savedCampaigns.single().first,
        )
        assertEquals(mapOf(result.campaignId to 2), recipientCounts)
        assertEquals(mapOf(result.campaignId to 2), enqueuedCounts)
        assertEquals(2, result.recipientCount)
        assertEquals(2, result.enqueuedCount)
    }

    @Test
    fun `대상이 없으면 발송을 요청하지 않고 0명으로 기록한다`() {
        tokens = emptyList()

        val result = send()

        assertTrue(requestedMessages.isEmpty())
        assertEquals(mapOf(result.campaignId to 0), recipientCounts)
        assertEquals(mapOf(result.campaignId to 0), enqueuedCounts)
        assertEquals(0, result.recipientCount)
        assertEquals(0, result.enqueuedCount)
    }

    @Test
    fun `일부만 큐에 넣었으면 넣은 수를 기록하고 돌려준다`() {
        enqueuedCount = 1

        val result = send()

        assertEquals(2, result.recipientCount)
        assertEquals(1, result.enqueuedCount)
        assertEquals(mapOf(result.campaignId to 1), enqueuedCounts)
    }

    @Test
    fun `하나도 큐에 넣지 못해도 실패로 던지지 않고 0을 돌려준다`() {
        enqueuedCount = 0

        val result = send()

        assertEquals(2, result.recipientCount)
        assertEquals(0, result.enqueuedCount)
        assertEquals(1, savedCampaigns.size)
        assertEquals(mapOf(result.campaignId to 0), enqueuedCounts)
    }

    @Test
    fun `10분 안에 같은 제목과 본문으로 다시 보내면 409 로 거부한다`() {
        send()
        now = now.plus(Duration.ofMinutes(9))

        val exception = assertThrows(BusinessException::class.java) { send() }

        assertEquals(PushErrorCode.DUPLICATE_MARKETING_PUSH, exception.errorCode)
        assertEquals(409, exception.errorCode.status)
        assertEquals(1, requestedMessages.size)
    }

    @Test
    fun `10분이 지났거나 본문이 다르면 다시 보낼 수 있다`() {
        send()
        send(command.copy(body = "다른 본문"))
        now = now.plus(Duration.ofMinutes(10)).plusSeconds(1)
        send()

        assertEquals(3, requestedMessages.size)
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["http://example.com/event", "javascript:alert(1)", "https://", "https:///path", "not a url"],
    )
    fun `https 주소가 아닌 링크는 INVALID_INPUT 으로 거부하고 기록하지 않는다`(link: String) {
        val exception = assertThrows(BusinessException::class.java) { send(command.copy(link = link)) }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        assertTrue(savedCampaigns.isEmpty())
        assertTrue(requestedMessages.isEmpty())
    }

    @Test
    fun `페이로드 한도를 넘는 링크는 INVALID_INPUT 으로 거부한다`() {
        val exception =
            assertThrows(BusinessException::class.java) {
                send(command.copy(link = "https://example.com/" + "a".repeat(4000)))
            }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        assertTrue(savedCampaigns.isEmpty())
    }

    private fun kst(time: String): Instant = OffsetDateTime.parse("$time+09:00").toInstant()

    private companion object {
        const val LINK = "https://chilsami.example.com/events/1"
    }
}
