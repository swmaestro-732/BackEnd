package com.example.backend.notification.application.port.outbound

import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle
import java.time.Instant

interface NotificationPersistencePort {
    /** 묶음에 처음 보는 행동자만 인원으로 센다. 이미 센 행동자의 반복은 무시한다. */
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

    /**
     * [userId] 가 받은 알림과, [userId] 가 마지막 행동자이고 닉네임 스냅샷을 남기는 종류(댓글, 리뷰, 팔로우)의 묶음을 지운다.
     * 남는 묶음은 [userId] 를 행동자에서 빼 인원을 남은 사람 수로 맞추고 마지막 행동자를 남은 사람으로 바꾸며,
     * 남은 사람이 없으면 지운다.
     */
    fun deleteAllInvolving(userId: Long)

    /** [cutoff] 이전에 만든 알림을 지우고 지운 수를 돌려준다. */
    fun deleteCreatedBefore(cutoff: Instant): Int
}
