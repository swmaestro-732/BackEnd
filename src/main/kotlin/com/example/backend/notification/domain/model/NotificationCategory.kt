package com.example.backend.notification.domain.model

enum class NotificationCategory {
    LIKE,
    COMMENT,
    FOLLOW,
    REVIEW,

    /** 운영 알림(어드민 조치). 수신 설정과 관계없이 보낸다. */
    SYSTEM,
}
