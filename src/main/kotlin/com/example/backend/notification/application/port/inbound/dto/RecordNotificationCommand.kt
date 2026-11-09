package com.example.backend.notification.application.port.inbound.dto

import com.example.backend.notification.domain.model.NotificationType
import java.time.Instant

data class RecordNotificationCommand(
    val type: NotificationType,
    val actorId: Long,
    val targetId: Long,
    val occurredAt: Instant,
)

/** 운영 알림 기록 요청. 코스가 이미 숨김, 삭제 상태라 조회하지 않고 표시용 제목과 커버를 함께 받는다. */
data class RecordSystemNotificationCommand(
    val type: NotificationType,
    val recipientId: Long,
    val targetId: Long,
    val courseTitle: String?,
    val courseImageUrl: String?,
    val occurredAt: Instant,
)
