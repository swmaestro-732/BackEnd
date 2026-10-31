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
}
