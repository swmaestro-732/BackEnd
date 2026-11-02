package com.example.backend.user.application.port.outbound

import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import java.time.Instant

interface DevicePersistencePort {
    /**
     * 기기 행을 만들거나 토큰과 헤더 값을 갱신한다. [userId] 가 있으면 그 계정으로 연결하고, null 이면 기존 연결을 유지한다.
     * 같은 토큰을 가진 다른 기기 행은 토큰과 연결을 비운다.
     */
    fun saveToken(
        command: RegisterDeviceTokenCommand,
        userId: Long?,
    )

    /** 기기 행이 없으면 빈 행을 만들고, 잠근 뒤 현재 동의 상태를 돌려준다. */
    fun lockConsent(deviceId: String): DeviceConsent

    fun updateConsent(
        deviceId: String,
        marketingAgreedAt: Instant?,
        nightAgreedAt: Instant?,
    )

    fun findConsent(deviceId: String): DeviceConsent?

    /** 이 계정에 연결된 이 기기만 연결을 끊는다. 토큰과 동의는 남는다. */
    fun unlink(
        userId: Long,
        deviceId: String,
    )

    fun unlinkAllByUser(userId: Long)
}
