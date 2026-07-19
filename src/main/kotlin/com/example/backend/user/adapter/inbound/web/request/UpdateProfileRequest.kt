package com.example.backend.user.adapter.inbound.web.request

/** 모킹 요청 DTO — 프로필 수정. 모든 필드 선택(부분 수정). */
data class UpdateProfileRequest(
    val nickname: String? = null,
    val handle: String? = null,
    val profileImageUrl: String? = null,
    val bio: String? = null,
)
