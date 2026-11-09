package com.example.backend.notification.domain.model

/** 상수 이름은 DB 저장 계약이다. */
enum class NotificationType(
    val category: NotificationCategory,
    val targetType: NotificationTargetType,
) {
    COURSE_LIKED(NotificationCategory.LIKE, NotificationTargetType.COURSE),
    COURSE_COMMENTED(NotificationCategory.COMMENT, NotificationTargetType.COURSE),
    COURSE_REVIEWED(NotificationCategory.REVIEW, NotificationTargetType.COURSE),
    USER_FOLLOWED(NotificationCategory.FOLLOW, NotificationTargetType.USER),

    // 어드민이 작성자의 코스를 숨기거나 삭제했다. 행동자가 없어 display_actor_id 는 [NotificationBundle.SYSTEM_ACTOR_ID] 다.
    COURSE_HIDDEN_BY_ADMIN(NotificationCategory.SYSTEM, NotificationTargetType.COURSE),
    COURSE_DELETED_BY_ADMIN(NotificationCategory.SYSTEM, NotificationTargetType.COURSE),
}
