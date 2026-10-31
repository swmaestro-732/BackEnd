package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.DeviceTokenUseCase
import com.example.backend.notification.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.notification.application.port.outbound.DeviceTokenPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeviceTokenService(
    private val deviceTokenPersistencePort: DeviceTokenPersistencePort,
) : DeviceTokenUseCase {
    override fun register(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    ) = deviceTokenPersistencePort.upsert(userId, command)

    override fun unregister(
        userId: Long,
        token: String,
    ) = deviceTokenPersistencePort.deleteByUserAndToken(userId, token)
}
