package com.example.backend.notification.adapter.outbound.persistence

import com.example.backend.notification.adapter.outbound.persistence.exposed.repository.NotificationRepository
import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.domain.model.NotificationBundle
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.time.toKotlinInstant

@Component
class NotificationPersistenceAdapter(
    private val notificationRepository: NotificationRepository,
) : NotificationPersistencePort {
    override fun upsert(bundle: NotificationBundle) = notificationRepository.upsert(bundle)

    override fun findPage(
        userId: Long,
        cursor: Long?,
        limit: Int,
    ): List<NotificationSummary> = notificationRepository.findPage(userId, cursor, limit)

    override fun countUnread(userId: Long): Long = notificationRepository.countUnread(userId)

    override fun markRead(
        userId: Long,
        id: Long,
    ): Int = notificationRepository.markRead(userId, id)

    override fun markAllRead(userId: Long): Int = notificationRepository.markAllRead(userId)

    override fun deleteAllInvolving(userId: Long) = notificationRepository.deleteAllInvolving(userId)

    override fun deleteCreatedBefore(cutoff: Instant): Int =
        notificationRepository.deleteCreatedBefore(cutoff.toKotlinInstant())
}
