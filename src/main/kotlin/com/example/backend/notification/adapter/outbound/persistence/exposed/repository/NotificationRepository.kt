package com.example.backend.notification.adapter.outbound.persistence.exposed.repository

import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationTable
import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.case
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
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
                // 직전 행동자와 같으면(좋아요 토글 반복 등) 인원을 늘리지 않고 읽음 상태도 되돌리지 않는다.
                // ponytail: 직전 행동자만 비교하므로 A, B, A 순서면 A 가 두 번 센다. 정확한 고유 인원이 필요하면 행동자 목록을 따로 둔다.
                val sameActor = NotificationTable.lastActorId eq insertValue(NotificationTable.lastActorId)
                it[NotificationTable.actorCount] =
                    NotificationTable.actorCount + case().When(sameActor, intLiteral(0)).Else(intLiteral(1))
                it[NotificationTable.readAt] = case().When(sameActor, NotificationTable.readAt).Else(Op.nullOp())
                it[NotificationTable.lastActorId] = insertValue(NotificationTable.lastActorId)
                it[NotificationTable.actorNickname] = insertValue(NotificationTable.actorNickname)
                it[NotificationTable.actorProfileImageUrl] = insertValue(NotificationTable.actorProfileImageUrl)
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
            it[actorNickname] = bundle.payload.actorNickname
            it[actorProfileImageUrl] = bundle.payload.actorProfileImageUrl
            it[courseTitle] = bundle.payload.courseTitle
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
                actorProfileImageUrl = row[NotificationTable.actorProfileImageUrl],
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
}
