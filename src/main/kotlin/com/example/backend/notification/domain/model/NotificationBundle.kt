package com.example.backend.notification.domain.model

import java.time.Duration
import java.time.Instant

data class NotificationBundle(
    val userId: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType,
    val targetId: Long,
    val windowStart: Instant,
    /** "~님 외 N명"으로 보여 줄 대표 행동자. 기록 때는 방금 행동한 사람, 누가 빠지면 남은 사람 중 하나다. */
    val displayActorId: Long,
    /** 대표 행동자의 닉네임 스냅샷. 대표가 바뀌면 함께 바꾼다. */
    val actorNickname: String?,
    val courseTitle: String?,
    val courseImageUrl: String?,
) {
    companion object {
        val BUNDLE_WINDOW: Duration = Duration.ofMinutes(10)

        fun windowStart(occurredAt: Instant): Instant =
            Instant.ofEpochSecond(Math.floorDiv(occurredAt.epochSecond, BUNDLE_WINDOW.seconds) * BUNDLE_WINDOW.seconds)
    }
}
