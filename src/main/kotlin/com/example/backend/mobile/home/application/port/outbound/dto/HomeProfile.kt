package com.example.backend.mobile.home.application.port.outbound.dto

/** BFF 아웃바운드 출력 — 홈 헤더 프로필(user 도메인 응답을 BFF 안으로 복사한 격리 DTO). */
data class HomeProfile(
    val nickname: String,
    val profileImageUrl: String?,
)
