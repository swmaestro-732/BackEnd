package com.example.backend.user.application.port.inbound.dto

/**
 * 프로필 부분 수정 커맨드 — null 은 "변경 안 함"이다.
 * [areaCodes] 는 빈 리스트면 관심 지역 전체 삭제, null 이면 유지(다른 필드와 동일한 부분 수정 시맨틱).
 */
data class UpdateProfileCommand(
    val nickname: String? = null,
    val handle: String? = null,
    val profileImageUrl: String? = null,
    val areaCodes: List<String>? = null,
)
