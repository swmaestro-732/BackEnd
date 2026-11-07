package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand

interface DeviceTokenUseCase {
    fun register(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    )

    fun unregister(
        userId: Long,
        deviceId: String,
    )

    /** FCM 이 무효라고 응답한 토큰을 기기 행에서 비운다. 앱이 새 토큰을 등록하면 같은 행이 다시 채워진다. */
    fun clearInvalidTokens(fcmTokens: Collection<String>)
}
