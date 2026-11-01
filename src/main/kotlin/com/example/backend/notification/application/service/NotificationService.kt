package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.NotificationUseCase
import com.example.backend.notification.application.port.inbound.dto.NotificationSummaryPage
import com.example.backend.notification.application.port.inbound.dto.RecordNotificationCommand
import com.example.backend.notification.application.port.outbound.NotificationActorLookupPort
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationTargetLookupPort
import com.example.backend.notification.domain.model.NotificationBundle
import com.example.backend.notification.domain.model.NotificationPayload
import com.example.backend.notification.domain.model.NotificationSettings
import com.example.backend.notification.domain.model.NotificationTargetType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 활동을 수신자의 10분 묶음에 기록한다. 비동기 실행기 스레드에서 별도 트랜잭션을 시작한다.
 * 커밋과 실행 사이 인스턴스 종료 또는 큐 포화 시 알림이 유실될 수 있으며 최대 한 번만 시도한다.
 * 목록은 id 내림차순으로 고정하므로 기존 묶음이 갱신되어도 위로 이동하지 않는다.
 */
@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationTargetLookupPort: NotificationTargetLookupPort,
    private val notificationActorLookupPort: NotificationActorLookupPort,
    private val notificationPersistencePort: NotificationPersistencePort,
    private val notificationSettingsPersistencePort: NotificationSettingsPersistencePort,
) : NotificationUseCase {
    @Transactional
    override fun record(activity: RecordNotificationCommand) {
        val targetType = activity.type.targetType
        val course =
            when (targetType) {
                NotificationTargetType.COURSE -> notificationTargetLookupPort.findCourse(activity.targetId) ?: return
                NotificationTargetType.USER -> null
            }
        val recipientId = course?.authorId ?: activity.targetId
        if (recipientId == activity.actorId) return
        if (!(
                notificationSettingsPersistencePort.findByUser(
                    recipientId,
                ) ?: NotificationSettings.DEFAULT
            ).allows(activity.type)
        ) {
            return
        }
        val actor = notificationActorLookupPort.findActor(activity.actorId) ?: return
        notificationPersistencePort.upsert(
            NotificationBundle(
                userId = recipientId,
                type = activity.type,
                targetType = targetType,
                targetId = activity.targetId,
                windowStart = NotificationBundle.windowStart(activity.occurredAt),
                lastActorId = activity.actorId,
                payload = NotificationPayload(actor.nickname, actor.profileImageUrl, course?.title),
            ),
        )
    }

    override fun list(
        userId: Long,
        cursor: Long?,
        size: Int,
    ): NotificationSummaryPage {
        val effectiveSize = size.coerceIn(1, 50)
        val rows = notificationPersistencePort.findPage(userId, cursor, effectiveSize + 1)
        val items = rows.take(effectiveSize)
        val hasNext = rows.size > effectiveSize
        return NotificationSummaryPage(items, hasNext, if (hasNext) items.last().id else null)
    }

    override fun unreadCount(userId: Long): Long = notificationPersistencePort.countUnread(userId)

    @Transactional
    override fun markRead(
        userId: Long,
        id: Long,
    ) {
        notificationPersistencePort.markRead(userId, id)
    }

    @Transactional
    override fun markAllRead(userId: Long): Int = notificationPersistencePort.markAllRead(userId)
}
