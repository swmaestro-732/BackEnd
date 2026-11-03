package com.example.backend.notification.domain.model

/** 상수 이름은 DB 저장 계약이다. */
enum class NotificationType(
    val category: NotificationCategory,
    val targetType: NotificationTargetType,
    /** false 면 누가 했는지 남기지 않고 활동 횟수만 보여 준다(좋아요). */
    val showsActor: Boolean,
) {
    COURSE_LIKED(NotificationCategory.LIKE, NotificationTargetType.COURSE, showsActor = false),
    COURSE_COMMENTED(NotificationCategory.COMMENT, NotificationTargetType.COURSE, showsActor = true),
    COURSE_REVIEWED(NotificationCategory.REVIEW, NotificationTargetType.COURSE, showsActor = true),
    USER_FOLLOWED(NotificationCategory.FOLLOW, NotificationTargetType.USER, showsActor = true),
}
