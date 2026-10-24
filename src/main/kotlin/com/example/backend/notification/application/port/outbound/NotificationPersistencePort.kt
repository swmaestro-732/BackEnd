package com.example.backend.notification.application.port.outbound

import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle

interface NotificationPersistencePort {
    /** 새 묶음 삽입이면 true, 기존 묶음 갱신이면 false. */
    fun upsert(bundle: NotificationBundle): Boolean

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
