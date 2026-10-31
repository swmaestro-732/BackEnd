package com.example.backend.user.application.port.outbound

import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand

interface DeviceTokenPersistencePort {
    fun upsert(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    )

    fun deleteByUserAndDevice(
        userId: Long,
        deviceId: String,
    )

    fun deleteAllByUser(userId: Long)
}
