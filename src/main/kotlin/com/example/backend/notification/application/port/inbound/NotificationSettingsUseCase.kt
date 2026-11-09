package com.example.backend.notification.application.port.inbound

import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.domain.model.NotificationSettings

interface NotificationSettingsUseCase {
    fun get(userId: Long): NotificationSettings

    fun update(
        userId: Long,
        command: UpdateNotificationSettingsCommand,
    ): NotificationSettings
}
