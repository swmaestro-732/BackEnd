package com.example.backend.push.adapter.outbound.user

import com.example.backend.push.application.port.outbound.PushTokenCleanupPort
import com.example.backend.user.application.port.inbound.DeviceTokenUseCase
import org.springframework.stereotype.Component

/** [PushTokenCleanupPort] 구현. user 도메인 인바운드 포트([DeviceTokenUseCase])에 위임한다. */
@Component
class PushTokenCleanupAdapter(
    private val deviceTokenUseCase: DeviceTokenUseCase,
) : PushTokenCleanupPort {
    override fun removeInvalid(tokens: Collection<String>) = deviceTokenUseCase.clearInvalidTokens(tokens)
}
