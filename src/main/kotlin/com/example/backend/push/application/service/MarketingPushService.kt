package com.example.backend.push.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.PushErrorCode
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
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId

/**
 * 광고성 정보 전송 규정(정보통신망법 제50조)에 맞춰 광고 표기, 수신거부 안내, 야간 동의를 적용해 발송한다.
 * 제목에는 서비스명([커미])을, 본문 앞에는 (광고)를 붙인다.
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
        val now = clock.instant()
        val kstTime = now.atZone(KST).toLocalTime()
        // 주간 대상으로 보낸 광고가 큐와 FCM 지연으로 21시 넘어 도착하지 않도록 직전 30분은 받지 않는다.
        if (kstTime >= BLOCK_START && kstTime < NIGHT_START) {
            throw BusinessException(CommonErrorCode.INVALID_INPUT, "20시 30분부터 21시까지는 광고 발송을 할 수 없습니다")
        }
        val night = kstTime >= NIGHT_START || kstTime < NIGHT_END
        val message =
            try {
                PushMessage(
                    tokens = emptyList(),
                    title = TITLE_PREFIX + command.title,
                    body = AD_PREFIX + command.body + OPT_OUT_LINE,
                    data = mapOf("type" to "MARKETING", "link" to command.link),
                    androidTtl = MARKETING_TTL,
                )
            } catch (e: IllegalArgumentException) {
                throw BusinessException(
                    CommonErrorCode.INVALID_INPUT,
                    e.message ?: CommonErrorCode.INVALID_INPUT.message,
                )
            }
        // 버튼 연타와 재시도로 같은 광고가 두 번 나가는 것을 막는다. 동시 요청까지 막는 잠금은 아니다.
        if (pushCampaignPersistencePort.existsRecent(
                command.adminEmail,
                message.title,
                message.body,
                now - DUPLICATE_WINDOW,
            )
        ) {
            throw BusinessException(PushErrorCode.DUPLICATE_MARKETING_PUSH)
        }
        // ponytail: 대상 토큰 전체를 메모리에 올리고 요청 스레드에서 SQS 로 순차 발행한다.
        // 동의 기기가 10만 대 안팎이 되면 키셋 페이징 조회와 비동기 발행으로 바꾼다.
        val tokens = marketingRecipientPort.findTokens(night)
        val campaignId =
            pushCampaignPersistencePort.save(
                PushCampaign(command.adminEmail, message.title, message.body, command.link, night, tokens.size),
            )
        val enqueuedCount = if (tokens.isEmpty()) 0 else pushUseCase.request(message.copy(tokens = tokens))
        if (enqueuedCount > 0) pushCampaignPersistencePort.updateEnqueuedCount(campaignId, enqueuedCount)
        if (tokens.isNotEmpty() && enqueuedCount == 0) {
            log.error { "마케팅 푸시를 큐에 하나도 넣지 못함: campaignId=$campaignId, count=${tokens.size}" }
        }
        log.info {
            "마케팅 푸시 발송 요청: campaignId=$campaignId, night=$night, count=${tokens.size}, enqueued=$enqueuedCount"
        }
        return MarketingPushResult(campaignId, tokens.size, enqueuedCount)
    }

    // 앱이 링크를 그대로 열기 때문에 https 주소만 받는다. javascript:, http 등은 거부한다.
    private fun validateLink(link: String) {
        val uri = runCatching { URI(link) }.getOrNull()
        if (!uri?.scheme.equals("https", ignoreCase = true) || uri?.host.isNullOrBlank()) {
            throw BusinessException(CommonErrorCode.INVALID_INPUT, "링크는 https 주소여야 합니다")
        }
    }

    private companion object {
        const val TITLE_PREFIX = "[커미] "
        const val AD_PREFIX = "(광고) "
        const val OPT_OUT_LINE = "\n수신거부: 마이 > 설정 > 알림"
        val BLOCK_START: LocalTime = LocalTime.of(20, 30)
        val NIGHT_START: LocalTime = LocalTime.of(21, 0)
        val NIGHT_END: LocalTime = LocalTime.of(8, 0)

        // 늦게 도착한 광고가 야간에 뜨지 않도록 기기 보관과 큐 보관을 30분으로 줄인다.
        val MARKETING_TTL: Duration = Duration.ofMinutes(30)
        val DUPLICATE_WINDOW: Duration = Duration.ofMinutes(10)
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
