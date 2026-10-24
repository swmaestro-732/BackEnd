package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.NotificationSettingsUseCase
import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import com.example.backend.notification.domain.model.NotificationSettings
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationSettingsService(
    private val settings: NotificationSettingsPersistencePort,
) : NotificationSettingsUseCase {
    override fun get(userId: Long): NotificationSettings = settings.findByUser(userId) ?: NotificationSettings.DEFAULT

    @Transactional
    override fun update(
        userId: Long,
        command: UpdateNotificationSettingsCommand,
    ): NotificationSettings = settings.update(userId, command)
}
