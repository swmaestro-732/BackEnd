package com.example.backend.push.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
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
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MarketingPushServiceTest {
    private var tokens = listOf("token-1", "token-2")
    private val requestedNights = mutableListOf<Boolean>()
    private val savedCampaigns = mutableListOf<PushCampaign>()
    private val requestedMessages = mutableListOf<PushMessage>()

    private val marketingRecipientPort =
        object : MarketingRecipientPort {
            override fun findTokens(night: Boolean): List<String> {
                requestedNights.add(night)
                return tokens
            }
        }

    private val pushCampaignPersistencePort =
        object : PushCampaignPersistencePort {
            override fun save(campaign: PushCampaign): Long {
                savedCampaigns.add(campaign)
                return CAMPAIGN_ID
            }
        }

    private val pushUseCase =
        object : PushUseCase {
            override fun request(message: PushMessage) {
                requestedMessages.add(message)
            }
        }

    private fun service(kst: String = "2026-11-04T12:00") =
        MarketingPushService(
            marketingRecipientPort,
            pushCampaignPersistencePort,
            pushUseCase,
            Clock.fixed(OffsetDateTime.parse("$kst+09:00").toInstant(), ZoneOffset.UTC),
        )

    private val command = SendMarketingPushCommand("admin@example.com", "가을 코스", "단풍 코스를 만나보세요", LINK)

    @ParameterizedTest
    @CsvSource("2026-11-04T20:59, false", "2026-11-04T21:00, true", "2026-11-05T07:59, true", "2026-11-05T08:00, false")
    fun `KST 21시부터 8시 전까지는 야간 동의 기기만 대상으로 조회한다`(
        kst: String,
        night: Boolean,
    ) {
        service(kst).send(command)

        assertEquals(listOf(night), requestedNights)
        assertEquals(night, savedCampaigns.single().night)
    }

    @Test
    fun `광고 표기와 수신거부 안내를 붙이고 data 에 type 과 link 를 담아 발송을 요청한다`() {
        val result = service().send(command)

        val message = requestedMessages.single()
        assertEquals(listOf("token-1", "token-2"), message.tokens)
        assertEquals("(광고) 가을 코스", message.title)
        assertEquals("단풍 코스를 만나보세요\n수신거부: 마이 > 설정 > 알림", message.body)
        assertEquals(mapOf("type" to "MARKETING", "link" to LINK), message.data)
        assertEquals(
            PushCampaign("admin@example.com", message.title, message.body, LINK, false, 2),
            savedCampaigns.single(),
        )
        assertEquals(CAMPAIGN_ID, result.campaignId)
        assertEquals(2, result.recipientCount)
    }

    @Test
    fun `대상이 없으면 발송을 요청하지 않고 0명으로 기록한다`() {
        tokens = emptyList()

        val result = service().send(command)

        assertTrue(requestedMessages.isEmpty())
        assertEquals(0, savedCampaigns.single().recipientCount)
        assertEquals(0, result.recipientCount)
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["http://example.com/event", "javascript:alert(1)", "https://", "https:///path", "not a url"],
    )
    fun `https 주소가 아닌 링크는 INVALID_INPUT 으로 거부하고 기록하지 않는다`(link: String) {
        val exception = assertThrows(BusinessException::class.java) { service().send(command.copy(link = link)) }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        assertTrue(savedCampaigns.isEmpty())
        assertTrue(requestedMessages.isEmpty())
    }

    @Test
    fun `페이로드 한도를 넘는 링크는 INVALID_INPUT 으로 거부한다`() {
        val exception =
            assertThrows(BusinessException::class.java) {
                service().send(command.copy(link = "https://example.com/" + "a".repeat(4000)))
            }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        assertTrue(savedCampaigns.isEmpty())
    }

    private companion object {
        const val CAMPAIGN_ID = 7L
        const val LINK = "https://chilsami.example.com/events/1"
    }
}
