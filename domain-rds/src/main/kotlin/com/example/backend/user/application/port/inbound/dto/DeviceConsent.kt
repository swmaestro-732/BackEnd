package com.example.backend.user.application.port.inbound.dto

import java.time.Instant

/** 기기의 광고성 수신 동의 상태. 시각이 null 이면 미동의. */
data class DeviceConsent(
    val marketingAgreedAt: Instant?,
    val nightAgreedAt: Instant?,
) {
    companion object {
        val NONE = DeviceConsent(null, null)
    }
}
