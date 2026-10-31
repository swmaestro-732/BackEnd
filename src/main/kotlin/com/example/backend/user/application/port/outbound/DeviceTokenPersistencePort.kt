package com.example.backend.user.application.port.outbound

import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand

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
