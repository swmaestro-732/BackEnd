package com.example.backend.notification.application.port.inbound

import com.example.backend.notification.application.port.inbound.dto.NotificationSummaryPage
import com.example.backend.notification.application.port.inbound.dto.RecordNotificationCommand
import com.example.backend.notification.application.port.inbound.dto.RecordSystemNotificationCommand

interface NotificationUseCase {
    fun record(activity: RecordNotificationCommand)

    /** 행동자 없는 운영 알림(어드민 조치)을 받는 사람에게 바로 남긴다. 코스 상태와 수신 설정은 보지 않는다. */
    fun recordSystem(command: RecordSystemNotificationCommand)

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
