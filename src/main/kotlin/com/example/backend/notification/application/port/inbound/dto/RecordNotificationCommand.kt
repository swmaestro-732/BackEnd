package com.example.backend.notification.application.port.inbound.dto

import com.example.backend.notification.domain.model.NotificationType
import java.time.Instant

data class RecordNotificationCommand(
    val type: NotificationType,
    val actorId: Long,
    val targetId: Long,
    val occurredAt: Instant,
)
