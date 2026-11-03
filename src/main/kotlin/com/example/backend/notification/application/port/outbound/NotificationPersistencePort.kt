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
     * 좋아요 묶음은 남기고 [userId] 의 행동자 행만 지운다(인원 수는 줄이지 않는다).
     */
    fun deleteAllInvolving(userId: Long)

    /** [cutoff] 이전에 만든 알림을 지우고 지운 수를 돌려준다. */
    fun deleteCreatedBefore(cutoff: Instant): Int
}
