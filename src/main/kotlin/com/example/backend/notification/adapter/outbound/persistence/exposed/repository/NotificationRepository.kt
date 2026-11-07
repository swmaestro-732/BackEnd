package com.example.backend.notification.adapter.outbound.persistence.exposed.repository

import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationActorTable
import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationTable
import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle
import com.example.backend.notification.domain.model.NotificationType
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.max
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
                it[displayActorId] = bundle.displayActorId
                it[actorCount] = 1
                it[actorNickname] = bundle.actorNickname
                it[courseTitle] = bundle.courseTitle
                it[courseImageUrl] = bundle.courseImageUrl
            }
        if (createdId != null) {
            insertActor(createdId.value, bundle.displayActorId)
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
        if (insertActor(id, bundle.displayActorId) == 0) return
        NotificationTable.update({ NotificationTable.id eq id }) {
            it[actorCount] = actorCount + 1
            it[readAt] = null
            it[displayActorId] = bundle.displayActorId
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
                displayActorId = row[NotificationTable.displayActorId],
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
     * 받은 알림을 지우고, 나머지 묶음에서는 [userId] 를 [detachActor] 로 뺀다.
     * 닉네임 스냅샷은 호출한 쪽이 돌려받은 새 대표 행동자로 맞춘다.
     */
    fun deleteAllInvolving(userId: Long): Map<Long, Long> {
        NotificationTable.deleteWhere { NotificationTable.userId eq userId }
        return detachActor(userId, null)
    }

    /** 같은 대상의 모든 창 묶음에서 [actorId] 를 뺀다. 지우는 규칙은 [detachActor] 와 같다. */
    fun removeActor(
        actorId: Long,
        type: NotificationType,
        targetId: Long,
        userId: Long?,
    ): Map<Long, Long> {
        var scope = (NotificationTable.type eq type) and (NotificationTable.targetId eq targetId)
        if (userId != null) scope = scope and (NotificationTable.userId eq userId)
        return detachActor(actorId, scope)
    }

    fun updateActorNickname(
        id: Long,
        nickname: String?,
    ) {
        NotificationTable.update({ NotificationTable.id eq id }) { it[actorNickname] = nickname }
    }

    /**
     * [scope] 안의 묶음(null 이면 전부)에서 [actorId] 의 행동자 행을 지우고 인원을 남은 사람 수로 맞춘다.
     * 대표 행동자가 [actorId] 였으면 남은 행동자 중 id 가 가장 큰 사람으로 바꾸고, 남은 행동자가 없으면 묶음을 지운다.
     * 대표 행동자를 바꾼 묶음의 id 와 새 대표 행동자를 돌려준다.
     */
    private fun detachActor(
        actorId: Long,
        scope: Op<Boolean>?,
    ): Map<Long, Long> {
        // 묶음 id 와 현재 대표 행동자. 묶음 행을 잠가 아래의 인원, 대표 행동자 덮어쓰기가 동시 upsert 와 겹치지 않게 한다
        // (upsert 의 행동자 INSERT 는 외래 키 검사로 묶음 행 잠금을 기다린다).
        val affected =
            NotificationActorTable
                .join(NotificationTable, JoinType.INNER, NotificationActorTable.notificationId, NotificationTable.id)
                .select(NotificationTable.id, NotificationTable.displayActorId)
                .where {
                    val byActor = NotificationActorTable.actorId eq actorId
                    if (scope == null) byActor else byActor and scope
                }.forUpdate()
                .associate { it[NotificationTable.id].value to it[NotificationTable.displayActorId] }
        if (affected.isEmpty()) return emptyMap()
        NotificationActorTable.deleteWhere {
            (NotificationActorTable.actorId eq actorId) and (NotificationActorTable.notificationId inList affected.keys)
        }
        val actorCount = NotificationActorTable.actorId.count()
        val maxActorId = NotificationActorTable.actorId.max()
        val remaining =
            NotificationActorTable
                .select(NotificationActorTable.notificationId, actorCount, maxActorId)
                .where { NotificationActorTable.notificationId inList affected.keys }
                .groupBy(NotificationActorTable.notificationId)
                .associate { it[NotificationActorTable.notificationId] to (it[actorCount].toInt() to it[maxActorId]!!) }
        val emptied = affected.keys - remaining.keys
        if (emptied.isNotEmpty()) NotificationTable.deleteWhere { NotificationTable.id inList emptied }
        // 묶음마다 UPDATE 한 번이지만 탈퇴나 취소 한 번에 묶음이 많지 않아 둔다. 느려지면 집계 서브쿼리 UPDATE 한 문장으로 바꾼다.
        remaining.forEach { (id, counted) ->
            val (count, latestActorId) = counted
            NotificationTable.update({ NotificationTable.id eq id }) {
                it[NotificationTable.actorCount] = count
                if (affected[id] == actorId) it[displayActorId] = latestActorId
            }
        }
        return remaining.filterKeys { affected[it] == actorId }.mapValues { it.value.second }
    }

    fun deleteCreatedBefore(cutoff: kotlin.time.Instant): Int =
        NotificationTable.deleteWhere { NotificationTable.createdAt less cutoff }
}
