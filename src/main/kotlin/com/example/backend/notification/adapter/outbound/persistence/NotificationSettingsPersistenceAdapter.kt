package com.example.backend.notification.adapter.outbound.persistence

import com.example.backend.notification.adapter.outbound.persistence.exposed.repository.NotificationSettingsRepository
import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import com.example.backend.notification.domain.model.NotificationSettings
import org.springframework.stereotype.Component

@Component
class NotificationSettingsPersistenceAdapter(
    private val repository: NotificationSettingsRepository,
) : NotificationSettingsPersistencePort {
    override fun findByUser(userId: Long): NotificationSettings? = repository.findByUser(userId)

    override fun delete(userId: Long) = repository.delete(userId)

    override fun update(
        userId: Long,
        command: UpdateNotificationSettingsCommand,
    ): NotificationSettings = repository.update(userId, command)
}
