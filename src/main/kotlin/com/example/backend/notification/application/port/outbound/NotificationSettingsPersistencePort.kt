package com.example.backend.notification.application.port.outbound

import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.domain.model.NotificationSettings

interface NotificationSettingsPersistencePort {
    fun findByUser(userId: Long): NotificationSettings?

    fun delete(userId: Long)

    fun update(
        userId: Long,
        command: UpdateNotificationSettingsCommand,
    ): NotificationSettings
}
