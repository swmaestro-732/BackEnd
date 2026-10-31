package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.notification.application.port.inbound.NotificationCleanupUseCase
import com.example.backend.user.application.port.outbound.NotificationCleanupPort
import org.springframework.stereotype.Component

@Component
class NotificationCleanupAdapter(
    private val notificationCleanupUseCase: NotificationCleanupUseCase,
) : NotificationCleanupPort {
    override fun purgeByUser(userId: Long) = notificationCleanupUseCase.purgeByUser(userId)
}
