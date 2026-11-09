package com.example.backend.course.application.port.inbound

enum class CourseActivityKind {
    LIKED,
    COMMENTED,
    REVIEWED,

    /** 좋아요 취소. 알림 묶음에서 행동자를 뺀다. */
    UNLIKED,

    /** 리뷰 삭제. 알림 묶음에서 행동자를 뺀다. */
    REVIEW_DELETED,

    ;

    fun isRetraction(): Boolean = this == UNLIKED || this == REVIEW_DELETED
}
