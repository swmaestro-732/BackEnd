package com.example.backend.notification.application.port.outbound

import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle
import com.example.backend.notification.domain.model.NotificationType
import java.time.Instant

interface NotificationPersistencePort {
    /** 묶음에 처음 보는 행동자만 인원으로 센다. 이미 센 행동자의 반복은 무시한다. */
    fun upsert(bundle: NotificationBundle)

    /**
     * [type] 과 [targetId] (와 [userId]) 가 같은 모든 창의 묶음에서 [actorId] 를 빼고 인원을 남은 사람 수로 맞추며,
     * 남은 사람이 없으면 지운다. [actorId] 가 마지막 행동자였고 남은 사람이 있는 묶음의 id 와 새 마지막 행동자를 돌려준다.
     */
    fun removeActor(
        actorId: Long,
        type: NotificationType,
        targetId: Long,
        userId: Long?,
    ): Map<Long, Long>

    fun updateActorNickname(
        id: Long,
        nickname: String?,
    )

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

    /**
     * [userId] 가 받은 알림과, [userId] 가 마지막 행동자라 닉네임 스냅샷이 남은 묶음을 지운다.
     * 남는 묶음은 [userId] 를 행동자에서 빼 인원을 남은 사람 수로 맞추고, 남은 사람이 없으면 지운다.
     */
    fun deleteAllInvolving(userId: Long)

    /** [cutoff] 이전에 만든 알림을 지우고 지운 수를 돌려준다. */
    fun deleteCreatedBefore(cutoff: Instant): Int
}
