package com.example.backend.notification.adapter.outbound.persistence.exposed.repository

import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationActorTable
import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationTable
import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle
import com.example.backend.notification.domain.model.NotificationType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.insertIgnoreAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Repository
class NotificationRepository {
    /**
     * 묶음 행이 없으면 행동자 1명으로 만들고, 있으면 처음 보는 행동자일 때만 인원을 1 늘리고 미읽음으로 돌린다.
     * 이미 센 행동자가 다시 하면 행을 건드리지 않는다. 동시 기록은 묶음 유니크 제약과 행동자 PK 로 한 번만 센다.
     */
    fun upsert(bundle: NotificationBundle) {
        val createdId =
            NotificationTable.insertIgnoreAndGetId {
                it[userId] = bundle.userId
                it[type] = bundle.type
                it[targetType] = bundle.targetType
                it[targetId] = bundle.targetId
                it[windowStart] = bundle.windowStart.toKotlinInstant()
                it[lastActorId] = bundle.lastActorId
                it[actorCount] = 1
                it[actorNickname] = bundle.actorNickname
                it[courseTitle] = bundle.courseTitle
                it[courseImageUrl] = bundle.courseImageUrl
            }
        if (createdId != null) {
            insertActor(createdId.value, bundle.lastActorId)
            return
        }
        val id =
            NotificationTable
                .select(NotificationTable.id)
                .where {
                    (NotificationTable.userId eq bundle.userId) and
                        (NotificationTable.type eq bundle.type) and
                        (NotificationTable.targetType eq bundle.targetType) and
                        (NotificationTable.targetId eq bundle.targetId) and
                        (NotificationTable.windowStart eq bundle.windowStart.toKotlinInstant())
                }.singleOrNull()
                ?.get(NotificationTable.id)
                ?.value ?: return
        if (insertActor(id, bundle.lastActorId) == 0) return
        NotificationTable.update({ NotificationTable.id eq id }) {
            it[actorCount] = actorCount + 1
            it[readAt] = null
            it[lastActorId] = bundle.lastActorId
            it[actorNickname] = bundle.actorNickname
            it[courseTitle] = bundle.courseTitle
            it[courseImageUrl] = bundle.courseImageUrl
            it[updatedAt] = CurrentTimestamp
        }
    }

    private fun insertActor(
        notificationId: Long,
        actorId: Long,
    ): Int =
        NotificationActorTable
            .insertIgnore {
                it[NotificationActorTable.notificationId] = notificationId
                it[NotificationActorTable.actorId] = actorId
            }.insertedCount

    fun findPage(
        userId: Long,
        cursor: Long?,
        limit: Int,
    ): List<NotificationSummary> {
        val query = NotificationTable.selectAll().where { NotificationTable.userId eq userId }
        if (cursor != null) query.andWhere { NotificationTable.id less cursor }
        return query.orderBy(NotificationTable.id, SortOrder.DESC).limit(limit).map { row ->
            NotificationSummary(
                id = row[NotificationTable.id].value,
                type = row[NotificationTable.type],
                targetType = row[NotificationTable.targetType],
                targetId = row[NotificationTable.targetId],
                actorCount = row[NotificationTable.actorCount],
                lastActorId = row[NotificationTable.lastActorId],
                actorNickname = row[NotificationTable.actorNickname],
                courseTitle = row[NotificationTable.courseTitle],
                courseImageUrl = row[NotificationTable.courseImageUrl],
                isRead = row[NotificationTable.readAt] != null,
                createdAt = row[NotificationTable.createdAt].toJavaInstant(),
                updatedAt = row[NotificationTable.updatedAt].toJavaInstant(),
            )
        }
    }

    fun countUnread(userId: Long): Long =
        NotificationTable
            .selectAll()
            .where { (NotificationTable.userId eq userId) and NotificationTable.readAt.isNull() }
            .count()

    fun markRead(
        userId: Long,
        id: Long,
    ): Int =
        NotificationTable.update({
            (NotificationTable.id eq id) and (NotificationTable.userId eq userId) and NotificationTable.readAt.isNull()
        }) {
            it[readAt] = CurrentTimestamp
        }

    fun markAllRead(userId: Long): Int =
        NotificationTable.update({
            (NotificationTable.userId eq userId) and NotificationTable.readAt.isNull()
        }) {
            it[readAt] = CurrentTimestamp
        }

    /**
     * 받은 알림과 닉네임 스냅샷을 남긴 묶음(마지막 행동자가 [userId] 이고 행동자를 보이는 종류)을 지운다.
     * 좋아요 묶음은 인원 수만 보이므로 남기고, 남는 묶음에서 [userId] 의 행동자 행만 지운다(인원 수는 줄이지 않는다).
     */
    fun deleteAllInvolving(userId: Long) {
        NotificationTable.deleteWhere {
            (NotificationTable.userId eq userId) or
                (
                    (NotificationTable.lastActorId eq userId) and
                        (NotificationTable.type inList NotificationType.entries.filter { it.showsActor })
                )
        }
        NotificationActorTable.deleteWhere { NotificationActorTable.actorId eq userId }
    }

    fun deleteCreatedBefore(cutoff: kotlin.time.Instant): Int =
        NotificationTable.deleteWhere { NotificationTable.createdAt less cutoff }
}
