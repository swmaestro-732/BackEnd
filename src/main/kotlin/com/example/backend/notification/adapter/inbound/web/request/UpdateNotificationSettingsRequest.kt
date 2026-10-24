package com.example.backend.notification.adapter.inbound.web.request

import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand

data class UpdateNotificationSettingsRequest(
    val activityLike: Boolean? = null,
    val activityComment: Boolean? = null,
    val activityFollow: Boolean? = null,
    val activityCourse: Boolean? = null,
    val activityReview: Boolean? = null,
) {
    fun toCommand(): UpdateNotificationSettingsCommand =
        UpdateNotificationSettingsCommand(activityLike, activityComment, activityFollow, activityCourse, activityReview)
}
