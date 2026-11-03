package com.example.backend.notification.adapter.outbound.persistence.exposed.repository

import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationTable
import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Repository
class NotificationRepository {
    fun upsert(bundle: NotificationBundle) {
        NotificationTable.upsert(
            NotificationTable.userId,
            NotificationTable.type,
            NotificationTable.targetType,
            NotificationTable.targetId,
            NotificationTable.windowStart,
            onUpdate = {
                // 묶인 활동 횟수를 늘리고, 새 활동이 생겼으니 다시 안 읽음으로 돌린다.
                it[NotificationTable.actorCount] = NotificationTable.actorCount + 1
                it[NotificationTable.readAt] = null
                it[NotificationTable.lastActorId] = insertValue(NotificationTable.lastActorId)
                it[NotificationTable.actorNickname] = insertValue(NotificationTable.actorNickname)
                it[NotificationTable.courseTitle] = insertValue(NotificationTable.courseTitle)
                it[NotificationTable.updatedAt] = CurrentTimestamp
            },
        ) {
            it[userId] = bundle.userId
            it[type] = bundle.type
            it[targetType] = bundle.targetType
            it[targetId] = bundle.targetId
            it[windowStart] = bundle.windowStart.toKotlinInstant()
            it[lastActorId] = bundle.lastActorId
            it[actorCount] = 1
            it[actorNickname] = bundle.actorNickname
            it[courseTitle] = bundle.courseTitle
        }
    }

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

    fun deleteAllInvolving(userId: Long) {
        NotificationTable.deleteWhere {
            (NotificationTable.userId eq userId) or
                (NotificationTable.lastActorId eq userId)
        }
    }

    fun deleteCreatedBefore(cutoff: kotlin.time.Instant): Int =
        NotificationTable.deleteWhere { NotificationTable.createdAt less cutoff }
}
