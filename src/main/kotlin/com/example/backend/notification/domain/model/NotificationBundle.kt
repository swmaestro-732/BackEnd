package com.example.backend.notification.domain.model

import java.time.Duration
import java.time.Instant

data class NotificationBundle(
    val userId: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType,
    val targetId: Long,
    val windowStart: Instant,
    val lastActorId: Long,
    val payload: NotificationPayload,
) {
    companion object {
        val BUNDLE_WINDOW: Duration = Duration.ofMinutes(10)

        fun windowStart(occurredAt: Instant): Instant =
            Instant.ofEpochSecond(Math.floorDiv(occurredAt.epochSecond, BUNDLE_WINDOW.seconds) * BUNDLE_WINDOW.seconds)
    }
}
