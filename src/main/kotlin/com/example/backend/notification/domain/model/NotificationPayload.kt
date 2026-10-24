package com.example.backend.notification.domain.model

/** 기록 시점의 프로필과 코스 제목 스냅샷. */
data class NotificationPayload(
    val actorNickname: String,
    val actorProfileImageUrl: String?,
    val courseTitle: String? = null,
)
