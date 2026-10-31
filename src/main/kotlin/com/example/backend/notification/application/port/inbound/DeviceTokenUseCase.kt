package com.example.backend.notification.application.port.inbound

import com.example.backend.notification.application.port.inbound.dto.RegisterDeviceTokenCommand

interface DeviceTokenUseCase {
    fun register(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    )

    fun unregister(
        userId: Long,
        token: String,
    )
}
