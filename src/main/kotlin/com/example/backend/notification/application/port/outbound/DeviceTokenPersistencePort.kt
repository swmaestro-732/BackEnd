package com.example.backend.notification.application.port.outbound

import com.example.backend.notification.application.port.inbound.dto.RegisterDeviceTokenCommand

interface DeviceTokenPersistencePort {
    fun upsert(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    )

    fun deleteByUserAndToken(
        userId: Long,
        token: String,
    )

    fun deleteAllByUser(userId: Long)
}
