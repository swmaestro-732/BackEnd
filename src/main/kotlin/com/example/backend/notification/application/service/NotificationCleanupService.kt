package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.NotificationCleanupUseCase
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationCleanupService(
    private val notifications: NotificationPersistencePort,
    private val settings: NotificationSettingsPersistencePort,
) : NotificationCleanupUseCase {
    @Transactional
    override fun purgeByUser(userId: Long) {
        notifications.deleteAllInvolving(userId)
        settings.delete(userId)
    }
}
