package com.example.backend.notification.domain.model

data class NotificationSettings(
    val activityLike: Boolean = true,
    val activityComment: Boolean = true,
    val activityFollow: Boolean = true,
    val activityReview: Boolean = true,
) {
    fun allows(type: NotificationType): Boolean =
        when (type.category) {
            NotificationCategory.LIKE -> activityLike
            NotificationCategory.COMMENT -> activityComment
            NotificationCategory.FOLLOW -> activityFollow
            NotificationCategory.REVIEW -> activityReview
            NotificationCategory.SYSTEM -> true
        }

    companion object {
        val DEFAULT = NotificationSettings()
    }
}
