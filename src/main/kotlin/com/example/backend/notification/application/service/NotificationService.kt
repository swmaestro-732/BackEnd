package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.NotificationUseCase
import com.example.backend.notification.application.port.inbound.dto.NotificationSummaryPage
import com.example.backend.notification.application.port.inbound.dto.RecordNotificationCommand
import com.example.backend.notification.application.port.outbound.NotificationActorLookupPort
import com.example.backend.notification.application.port.outbound.NotificationNicknamePort
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationTargetLookupPort
import com.example.backend.notification.domain.model.NotificationBundle
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
    private val notificationNicknamePort: NotificationNicknamePort,
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
        // 행동자와 받는 사람의 사용자 행을 잠가 탈퇴와 직렬화한다. 둘 중 하나라도 이미 탈퇴했으면 기록하지 않는다.
        val participants = listOf(activity.actorId, recipientId)
        if (!notificationActorLookupPort.lockActive(participants).containsAll(participants)) return
        val actorNickname = notificationNicknamePort.findNicknames(listOf(activity.actorId))[activity.actorId] ?: return
        notificationPersistencePort.upsert(
            NotificationBundle(
                userId = recipientId,
                type = activity.type,
                targetType = targetType,
                targetId = bundleTargetId(activity),
                windowStart = NotificationBundle.windowStart(activity.occurredAt),
                displayActorId = activity.actorId,
                actorNickname = actorNickname,
                courseTitle = course?.title,
                courseImageUrl = course?.imageUrl,
            ),
        )
    }

    /**
     * 창과 상관없이 같은 대상의 묶음에서 행동자를 빼 다시 행동해도 알림이 하나만 남게 한다.
     * 묶음은 지우기만 하므로 코스 상태, 수신 설정, 탈퇴 잠금은 보지 않는다.
     * 한계: 좋아요 직후 취소하면 두 이벤트가 실행기 스레드에서 순서가 뒤바뀔 수 있고, 철회가 먼저 돌면 알림 1건이 남는다.
     * 문제가 되면 기록 시점에 현재 좋아요/팔로우 상태를 확인하거나 대상별로 순서를 보장한다.
     */
    @Transactional
    override fun retract(activity: RecordNotificationCommand) {
        // 팔로우 묶음은 받는 사람(targetId)마다 있으므로 받는 사람까지 맞춘다. 코스 묶음은 작성자 한 명만 받는다.
        val recipientId = if (activity.type.targetType == NotificationTargetType.USER) activity.targetId else null
        val displayActorByBundle =
            notificationPersistencePort.removeActor(
                activity.actorId,
                activity.type,
                bundleTargetId(activity),
                recipientId,
            )
        // 대표 행동자가 바뀐 묶음은 닉네임 스냅샷도 새 행동자로 맞춘다. 조회되지 않으면 비워 빠진 사람 닉네임을 남기지 않는다.
        val nicknames = notificationNicknamePort.findNicknames(displayActorByBundle.values.toSet())
        displayActorByBundle.forEach { (id, actorId) ->
            notificationPersistencePort.updateActorNickname(id, nicknames[actorId])
        }
    }

    // 사용자 대상 알림(팔로우)은 행동자를 대상으로 둬 행동자마다 따로 보이게 한다(맞팔로우 버튼, 프로필 이동).
    // 코스 알림은 코스를 대상으로 둬 같은 코스의 활동끼리 묶는다.
    private fun bundleTargetId(activity: RecordNotificationCommand): Long =
        when (activity.type.targetType) {
            NotificationTargetType.COURSE -> activity.targetId
            NotificationTargetType.USER -> activity.actorId
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
