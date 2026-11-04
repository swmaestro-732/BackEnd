package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import java.time.Instant

/** 앱은 동의, 철회 직후 이 시각으로 처리 결과를 안내한다. null 이면 미동의. */
data class DeviceConsentResponse(
    val marketingAgreedAt: Instant?,
    val nightAgreedAt: Instant?,
) {
    companion object {
        fun from(consent: DeviceConsent): DeviceConsentResponse =
            DeviceConsentResponse(consent.marketingAgreedAt, consent.nightAgreedAt)
    }
}
