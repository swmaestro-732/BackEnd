package com.example.backend.notification.adapter.inbound.web.response

import com.example.backend.notification.domain.model.NotificationSettings

data class NotificationSettingsResponse(
    val activityLike: Boolean,
    val activityComment: Boolean,
    val activityFollow: Boolean,
    val activityCourse: Boolean,
    val activityReview: Boolean,
) {
    companion object {
        fun from(settings: NotificationSettings): NotificationSettingsResponse =
            NotificationSettingsResponse(
                settings.activityLike,
                settings.activityComment,
                settings.activityFollow,
                settings.activityCourse,
                settings.activityReview,
            )
    }
}
