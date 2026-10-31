package com.example.backend.notification.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object NotificationSettingsTable : Table("notification_settings") {
    val userId = long("user_id")
    val activityLike = bool("activity_like")
    val activityComment = bool("activity_comment")
    val activityFollow = bool("activity_follow")
    val activityCourse = bool("activity_course")
    val activityReview = bool("activity_review")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(userId)
}
