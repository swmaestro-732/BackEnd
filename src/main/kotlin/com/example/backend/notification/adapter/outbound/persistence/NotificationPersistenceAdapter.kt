package com.example.backend.notification.adapter.outbound.persistence

import com.example.backend.notification.adapter.outbound.persistence.exposed.repository.NotificationRepository
import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.domain.model.NotificationBundle
import org.springframework.stereotype.Component

@Component
class NotificationPersistenceAdapter(
    private val repository: NotificationRepository,
) : NotificationPersistencePort {
    override fun upsert(bundle: NotificationBundle): Boolean = repository.upsert(bundle)

    override fun findPage(
        userId: Long,
        cursor: Long?,
        limit: Int,
    ): List<NotificationSummary> = repository.findPage(userId, cursor, limit)

    override fun countUnread(userId: Long): Long = repository.countUnread(userId)

    override fun markRead(
        userId: Long,
        id: Long,
    ): Int = repository.markRead(userId, id)

    override fun markAllRead(userId: Long): Int = repository.markAllRead(userId)

    override fun deleteAllInvolving(userId: Long) = repository.deleteAllInvolving(userId)
}
