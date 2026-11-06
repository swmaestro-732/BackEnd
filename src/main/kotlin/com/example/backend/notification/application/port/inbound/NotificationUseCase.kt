package com.example.backend.notification.application.port.inbound

import com.example.backend.notification.application.port.inbound.dto.NotificationSummaryPage
import com.example.backend.notification.application.port.inbound.dto.RecordNotificationCommand

interface NotificationUseCase {
    fun record(activity: RecordNotificationCommand)

    /** 취소된 행동(좋아요 취소, 언팔로우, 리뷰 삭제)의 행동자를 같은 대상의 모든 묶음에서 뺀다. */
    fun retract(activity: RecordNotificationCommand)

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
