package com.example.backend.notification.application.port.inbound

import com.example.backend.notification.application.port.inbound.dto.NotificationSummaryPage
import com.example.backend.notification.application.port.inbound.dto.RecordNotificationCommand

interface NotificationUseCase {
    fun record(activity: RecordNotificationCommand)

    fun list(
        userId: Long,
        cursor: Long?,
        size: Int,
    ): NotificationSummaryPage

    fun unreadCount(userId: Long): Long

    fun markRead(
        userId: Long,
        id: Long,
    )

    fun markAllRead(userId: Long): Int
}
