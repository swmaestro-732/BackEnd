package com.example.backend.notification.application.port.inbound.dto

data class UpdateNotificationSettingsCommand(
    val activityLike: Boolean? = null,
    val activityComment: Boolean? = null,
    val activityFollow: Boolean? = null,
    val activityCourse: Boolean? = null,
    val activityReview: Boolean? = null,
)
