package com.example.backend.notification.domain.model

/** 상수 이름은 DB 저장 계약이다. */
enum class NotificationType(
    val category: NotificationCategory,
    val targetType: NotificationTargetType,
) {
    COURSE_LIKED(NotificationCategory.LIKE, NotificationTargetType.COURSE),
    COURSE_COMMENTED(NotificationCategory.COMMENT, NotificationTargetType.COURSE),
    COURSE_TRACE_COMPLETED(NotificationCategory.COURSE, NotificationTargetType.COURSE),
    COURSE_REVIEWED(NotificationCategory.REVIEW, NotificationTargetType.COURSE),
    USER_FOLLOWED(NotificationCategory.FOLLOW, NotificationTargetType.USER),
    COURSE_BOOKMARKED(NotificationCategory.COURSE, NotificationTargetType.COURSE),
}
