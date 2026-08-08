package com.example.backend.user.application.port.inbound.dto

/** 유스케이스 출력 — 사용자 관심 테마 한 건. [name] 은 course 도메인 태그 마스터에서 푼 표시 이름. */
data class UserLikeTagResult(
    val id: Long,
    val name: String,
)
