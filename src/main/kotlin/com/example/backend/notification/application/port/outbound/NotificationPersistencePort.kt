package com.example.backend.notification.application.port.outbound

import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle

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
}
