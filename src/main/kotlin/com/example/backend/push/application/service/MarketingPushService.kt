package com.example.backend.push.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.push.application.port.inbound.MarketingPushUseCase
import com.example.backend.push.application.port.inbound.PushUseCase
import com.example.backend.push.application.port.inbound.dto.MarketingPushResult
import com.example.backend.push.application.port.inbound.dto.PushMessage
import com.example.backend.push.application.port.inbound.dto.SendMarketingPushCommand
import com.example.backend.push.application.port.outbound.MarketingRecipientPort
import com.example.backend.push.application.port.outbound.PushCampaign
import com.example.backend.push.application.port.outbound.PushCampaignPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URI
import java.time.Clock
import java.time.ZoneId

/**
 * 광고성 정보 전송 규정(정보통신망법 제50조)에 맞춰 광고 표기, 수신거부 안내, 야간 동의를 적용해 발송한다.
 * 트랜잭션으로 묶지 않는다. 발송 이력을 먼저 커밋해 두어, 큐 발행이 묶음 중간에 실패해도 이미 나간 광고의 기록이 남게 한다.
 */
@Service
class MarketingPushService(
    private val marketingRecipientPort: MarketingRecipientPort,
    private val pushCampaignPersistencePort: PushCampaignPersistencePort,
    private val pushUseCase: PushUseCase,
    private val clock: Clock,
) : MarketingPushUseCase {
    private val log = KotlinLogging.logger {}

    override fun send(command: SendMarketingPushCommand): MarketingPushResult {
        validateLink(command.link)
        val message =
            try {
                PushMessage(
                    tokens = emptyList(),
                    title = AD_PREFIX + command.title,
                    body = command.body + OPT_OUT_LINE,
                    data = mapOf("type" to "MARKETING", "link" to command.link),
                )
            } catch (e: IllegalArgumentException) {
                throw BusinessException(
                    CommonErrorCode.INVALID_INPUT,
                    e.message ?: CommonErrorCode.INVALID_INPUT.message,
                )
            }
        val night =
            clock
                .instant()
                .atZone(KST)
                .hour
                .let { it >= NIGHT_START_HOUR || it < NIGHT_END_HOUR }
        val tokens = marketingRecipientPort.findTokens(night)
        val campaignId =
            pushCampaignPersistencePort.save(
                PushCampaign(command.adminEmail, message.title, message.body, command.link, night, tokens.size),
            )
        if (tokens.isNotEmpty()) pushUseCase.request(message.copy(tokens = tokens))
        log.info { "마케팅 푸시 발송 요청: campaignId=$campaignId, night=$night, count=${tokens.size}" }
        return MarketingPushResult(campaignId, tokens.size)
    }

    // 앱이 링크를 그대로 열기 때문에 https 주소만 받는다. javascript:, http 등은 거부한다.
    private fun validateLink(link: String) {
        val uri = runCatching { URI(link) }.getOrNull()
        if (!uri?.scheme.equals("https", ignoreCase = true) || uri?.host.isNullOrBlank()) {
            throw BusinessException(CommonErrorCode.INVALID_INPUT, "링크는 https 주소여야 합니다")
        }
    }

    private companion object {
        const val AD_PREFIX = "(광고) "
        const val OPT_OUT_LINE = "\n수신거부: 마이 > 설정 > 알림"
        const val NIGHT_START_HOUR = 21
        const val NIGHT_END_HOUR = 8
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
