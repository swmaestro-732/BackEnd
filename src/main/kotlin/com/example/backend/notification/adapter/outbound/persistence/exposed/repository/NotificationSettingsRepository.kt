package com.example.backend.notification.adapter.outbound.persistence.exposed.repository

import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationSettingsTable
import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.domain.model.NotificationSettings
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository

@Repository
class NotificationSettingsRepository {
    fun findByUser(userId: Long): NotificationSettings? =
        NotificationSettingsTable
            .selectAll()
            .where { NotificationSettingsTable.userId eq userId }
            .singleOrNull()
            ?.let {
                NotificationSettings(
                    activityLike = it[NotificationSettingsTable.activityLike],
                    activityComment = it[NotificationSettingsTable.activityComment],
                    activityFollow = it[NotificationSettingsTable.activityFollow],
                    activityCourse = it[NotificationSettingsTable.activityCourse],
                    activityReview = it[NotificationSettingsTable.activityReview],
                )
            }

    fun update(
        userId: Long,
        command: UpdateNotificationSettingsCommand,
    ): NotificationSettings {
        val changes =
            listOf(
                command.activityLike,
                command.activityComment,
                command.activityFollow,
                command.activityCourse,
                command.activityReview,
            )
        // null 필드를 DB의 현재 값으로 유지해 동시 부분 수정끼리 서로 덮어쓰지 않는다.
        return checkNotNull(
            TransactionManager.current().exec(
                """
                INSERT INTO notification_settings (user_id, activity_like, activity_comment, activity_follow, activity_course, activity_review)
                VALUES (?, COALESCE(?, true), COALESCE(?, true), COALESCE(?, true), COALESCE(?, true), COALESCE(?, true))
                ON CONFLICT (user_id) DO UPDATE SET
                    activity_like = COALESCE(?, notification_settings.activity_like),
                    activity_comment = COALESCE(?, notification_settings.activity_comment),
                    activity_follow = COALESCE(?, notification_settings.activity_follow),
                    activity_course = COALESCE(?, notification_settings.activity_course),
                    activity_review = COALESCE(?, notification_settings.activity_review),
                    updated_at = now()
                RETURNING activity_like, activity_comment, activity_follow, activity_course, activity_review
                """.trimIndent(),
                args = listOf(LongColumnType() to userId) + (changes + changes).map { BooleanColumnType() to it },
                explicitStatementType = StatementType.SELECT,
            ) { rs ->
                check(rs.next()) { "알림 설정 업서트 결과가 없습니다." }
                NotificationSettings(
                    rs.getBoolean("activity_like"),
                    rs.getBoolean("activity_comment"),
                    rs.getBoolean("activity_follow"),
                    rs.getBoolean("activity_course"),
                    rs.getBoolean("activity_review"),
                )
            },
        )
    }

    fun delete(userId: Long) {
        NotificationSettingsTable.deleteWhere { NotificationSettingsTable.userId eq userId }
    }
}
