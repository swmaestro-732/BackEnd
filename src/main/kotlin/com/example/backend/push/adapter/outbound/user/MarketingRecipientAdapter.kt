package com.example.backend.push.adapter.outbound.user

import com.example.backend.push.application.port.outbound.MarketingRecipientPort
import com.example.backend.user.application.port.inbound.MarketingDeviceUseCase
import org.springframework.stereotype.Component

/** [MarketingRecipientPort] 구현. user 도메인 인바운드 포트([MarketingDeviceUseCase])에 위임한다. */
@Component
class MarketingRecipientAdapter(
    private val marketingDeviceUseCase: MarketingDeviceUseCase,
) : MarketingRecipientPort {
    override fun findTokens(night: Boolean): List<String> = marketingDeviceUseCase.findMarketingTokens(night)
}
