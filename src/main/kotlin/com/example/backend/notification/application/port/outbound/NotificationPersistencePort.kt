package com.example.backend.notification.application.port.outbound

import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle
import java.time.Instant

interface NotificationPersistencePort {
    fun upsert(bundle: NotificationBundle)

    fun findPage(
        userId: Long,
        cursor: Long?,
        limit: Int,
    ): List<NotificationSummary>

    fun countUnread(userId: Long): Long

    fun markRead(
        userId: Long,
        id: Long,
    ): Int

    fun markAllRead(userId: Long): Int

    /** 받는 사람이거나 마지막 행위자가 [userId] 인 알림을 지운다. */
    fun deleteAllInvolving(userId: Long)

    /** [cutoff] 이전에 만든 알림을 지우고 지운 수를 돌려준다. */
    fun deleteCreatedBefore(cutoff: Instant): Int
}
