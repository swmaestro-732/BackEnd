package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.NotificationCleanupUseCase
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class NotificationCleanupService(
    private val notificationPersistencePort: NotificationPersistencePort,
    private val notificationSettingsPersistencePort: NotificationSettingsPersistencePort,
    private val clock: Clock,
) : NotificationCleanupUseCase {
    @Transactional
    override fun purgeByUser(userId: Long) {
        notificationPersistencePort.deleteAllInvolving(userId)
        notificationSettingsPersistencePort.delete(userId)
    }

    @Transactional
    override fun purgeExpired(): Int =
        notificationPersistencePort.deleteCreatedBefore(clock.instant().minus(NotificationCleanupUseCase.RETENTION))
}
