package com.example.backend.notification.application.port.inbound.dto

import com.example.backend.notification.domain.model.NotificationTargetType
import com.example.backend.notification.domain.model.NotificationType
import java.time.Instant

data class NotificationSummary(
    val id: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType,
    val targetId: Long,
    val actorCount: Int,
    val lastActorId: Long,
    val actorNickname: String,
    val actorProfileImageUrl: String?,
    val courseTitle: String?,
    val isRead: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
