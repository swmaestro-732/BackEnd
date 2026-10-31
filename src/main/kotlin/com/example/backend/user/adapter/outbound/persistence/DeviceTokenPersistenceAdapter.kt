package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.DeviceTokenRepository
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.outbound.DeviceTokenPersistencePort
import org.springframework.stereotype.Component

@Component
class DeviceTokenPersistenceAdapter(
    private val deviceTokenRepository: DeviceTokenRepository,
) : DeviceTokenPersistencePort {
    override fun upsert(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    ) = deviceTokenRepository.upsert(userId, command)

    override fun deleteByUserAndToken(
        userId: Long,
        token: String,
    ) = deviceTokenRepository.deleteByUserAndToken(userId, token)

    override fun deleteAllByUser(userId: Long) = deviceTokenRepository.deleteAllByUser(userId)
}
