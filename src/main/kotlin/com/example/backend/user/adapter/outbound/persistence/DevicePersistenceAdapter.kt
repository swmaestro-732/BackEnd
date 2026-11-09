package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.DeviceRepository
import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import com.example.backend.user.application.port.outbound.LockedDevice
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate

@Component
class DevicePersistenceAdapter(
    private val deviceRepository: DeviceRepository,
) : DevicePersistencePort {
    override fun saveToken(
        command: RegisterDeviceTokenCommand,
        userId: Long?,
    ) = deviceRepository.saveToken(command, userId)

    override fun lockConsent(deviceId: String): LockedDevice = deviceRepository.lockConsent(deviceId)

    override fun updateConsent(
        deviceId: String,
        marketingAgreedAt: Instant?,
        nightAgreedAt: Instant?,
    ) = deviceRepository.updateConsent(deviceId, marketingAgreedAt, nightAgreedAt)

    override fun findConsent(deviceId: String): DeviceConsent? = deviceRepository.findConsent(deviceId)

    override fun unlink(
        userId: Long,
        deviceId: String,
    ) = deviceRepository.unlink(userId, deviceId)

    override fun unlinkAllByUser(userId: Long) = deviceRepository.unlinkAllByUser(userId)

    override fun recordActivity(
        deviceId: String,
        date: LocalDate,
        userId: Long?,
        platform: String?,
    ) = deviceRepository.recordActivity(deviceId, date, userId, platform)

    override fun clearTokens(fcmTokens: Collection<String>) = deviceRepository.clearTokens(fcmTokens)
}
