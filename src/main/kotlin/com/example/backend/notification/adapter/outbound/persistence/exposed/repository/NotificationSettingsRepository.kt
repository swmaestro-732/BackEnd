package com.example.backend.notification.adapter.outbound.persistence.exposed.repository

import com.example.backend.notification.adapter.outbound.persistence.exposed.NotificationSettingsTable
import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.domain.model.NotificationSettings
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import org.springframework.stereotype.Repository

@Repository
class NotificationSettingsRepository {
    fun findByUser(userId: Long): NotificationSettings? =
        NotificationSettingsTable
            .selectAll()
            .where { NotificationSettingsTable.userId eq userId }
            .singleOrNull()
            ?.toSettings()

    fun update(
        userId: Long,
        command: UpdateNotificationSettingsCommand,
    ): NotificationSettings {
        val changes =
            listOf(
                NotificationSettingsTable.activityLike to command.activityLike,
                NotificationSettingsTable.activityComment to command.activityComment,
                NotificationSettingsTable.activityFollow to command.activityFollow,
                NotificationSettingsTable.activityReview to command.activityReview,
            )
        // 갱신은 요청에 온 필드만 SET 한다. null 필드는 DB 의 현재 값을 그대로 둬 동시 부분 수정끼리 덮어쓰지 않는다.
        return NotificationSettingsTable
            .upsertReturning(
                NotificationSettingsTable.userId,
                onUpdate = {
                    changes.forEach { (column, value) -> if (value != null) it[column] = insertValue(column) }
                    it[NotificationSettingsTable.updatedAt] = CurrentTimestamp
                },
            ) {
                it[NotificationSettingsTable.userId] = userId
                changes.forEach { (column, value) -> it[column] = value ?: true }
            }.single()
            .toSettings()
    }

    fun delete(userId: Long) {
        NotificationSettingsTable.deleteWhere { NotificationSettingsTable.userId eq userId }
    }

    private fun ResultRow.toSettings(): NotificationSettings =
        NotificationSettings(
            activityLike = this[NotificationSettingsTable.activityLike],
            activityComment = this[NotificationSettingsTable.activityComment],
            activityFollow = this[NotificationSettingsTable.activityFollow],
            activityReview = this[NotificationSettingsTable.activityReview],
        )
}
