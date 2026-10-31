package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.NotificationCleanupUseCase
import com.example.backend.notification.application.port.outbound.DeviceTokenPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NotificationCleanupService(
    private val deviceTokenPersistencePort: DeviceTokenPersistencePort,
) : NotificationCleanupUseCase {
    override fun purgeByUser(userId: Long) = deviceTokenPersistencePort.deleteAllByUser(userId)
}
