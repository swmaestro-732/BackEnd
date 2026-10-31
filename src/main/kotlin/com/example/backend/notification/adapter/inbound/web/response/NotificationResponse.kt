package com.example.backend.notification.adapter.inbound.web.response

import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val type: String,
    val targetType: String,
    val targetId: Long,
    val actorCount: Int,
    val lastActorId: Long,
    val actorNickname: String,
    val actorProfileImageUrl: String?,
    val courseTitle: String?,
    val isRead: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(item: NotificationSummary): NotificationResponse =
            NotificationResponse(
                id = item.id,
                type = item.type.name,
                targetType = item.targetType.name,
                targetId = item.targetId,
                actorCount = item.actorCount,
                lastActorId = item.lastActorId,
                actorNickname = item.actorNickname,
                actorProfileImageUrl = item.actorProfileImageUrl,
                courseTitle = item.courseTitle,
                isRead = item.isRead,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
            )
    }
}
