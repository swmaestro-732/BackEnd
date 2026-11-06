package com.example.backend.notification.adapter.inbound.web.response

import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val type: String,
    val targetType: String,
    val targetId: Long,
    val actorCount: Int,
    /** 행동자를 보이지 않는 종류(좋아요)는 누가 했는지 숨기려 null 이다. */
    val lastActorId: Long?,
    val actorNickname: String?,
    val courseTitle: String?,
    val courseImageUrl: String?,
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
                lastActorId = item.lastActorId.takeIf { item.type.showsActor },
                actorNickname = item.actorNickname,
                courseTitle = item.courseTitle,
                courseImageUrl = item.courseImageUrl,
                isRead = item.isRead,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
            )
    }
}
