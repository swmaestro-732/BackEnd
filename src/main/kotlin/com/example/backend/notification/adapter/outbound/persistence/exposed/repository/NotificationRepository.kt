package com.example.backend.notification.adapter.outbound.persistence.exposed.repository

import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationTable
import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.domain.model.NotificationBundle
import com.example.backend.notification.domain.model.NotificationPayload
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import kotlin.time.toJavaInstant

@Repository
class NotificationRepository(
    private val objectMapper: ObjectMapper,
) {
    fun upsert(bundle: NotificationBundle): Boolean {
        val payload =
            linkedMapOf<String, Any?>(
                "actorNickname" to bundle.payload.actorNickname,
                "actorProfileImageUrl" to bundle.payload.actorProfileImageUrl,
            )
        bundle.payload.courseTitle?.let { payload["courseTitle"] = it }
        // shortcut: 같은 주체의 반복 활동도 합산하며 고유 주체 수가 필요해지면 별도 중복 추적을 도입한다.
        return checkNotNull(
            TransactionManager.current().exec(
                """
                INSERT INTO notifications (user_id, type, target_type, target_id, window_start, last_actor_id, payload)
                VALUES (?, ?, ?, ?, CAST(? AS timestamptz), ?, CAST(? AS jsonb))
                ON CONFLICT ON CONSTRAINT uq_notifications_bundle DO UPDATE SET
                    actor_count = notifications.actor_count + 1,
                    last_actor_id = EXCLUDED.last_actor_id,
                    payload = EXCLUDED.payload,
                    read_at = NULL,
                    updated_at = now()
                RETURNING (xmax = 0) AS inserted
                """.trimIndent(),
                args =
                    listOf(
                        LongColumnType() to bundle.userId,
                        VarCharColumnType() to bundle.type.name,
                        VarCharColumnType() to bundle.targetType.name,
                        LongColumnType() to bundle.targetId,
                        TextColumnType() to bundle.windowStart.toString(),
                        LongColumnType() to bundle.lastActorId,
                        TextColumnType() to objectMapper.writeValueAsString(payload),
                    ),
                explicitStatementType = StatementType.SELECT,
            ) { rs ->
                check(rs.next()) { "알림 업서트 결과가 없습니다." }
                rs.getBoolean("inserted")
            },
        )
    }

    fun findPage(
        userId: Long,
        cursor: Long?,
        limit: Int,
    ): List<NotificationSummary> {
        val query = NotificationTable.selectAll().where { NotificationTable.userId eq userId }
        if (cursor != null) query.andWhere { NotificationTable.id less cursor }
        return query.orderBy(NotificationTable.id, SortOrder.DESC).limit(limit).map { row ->
            val payload = objectMapper.readValue(row[NotificationTable.payload], NotificationPayload::class.java)
            NotificationSummary(
                id = row[NotificationTable.id].value,
                type = row[NotificationTable.type],
                targetType = row[NotificationTable.targetType],
                targetId = row[NotificationTable.targetId],
                actorCount = row[NotificationTable.actorCount],
                lastActorId = row[NotificationTable.lastActorId],
                actorNickname = payload.actorNickname,
                actorProfileImageUrl = payload.actorProfileImageUrl,
                courseTitle = payload.courseTitle,
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
