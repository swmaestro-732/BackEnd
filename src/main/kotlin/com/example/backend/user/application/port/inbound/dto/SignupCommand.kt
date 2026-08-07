package com.example.backend.user.application.port.inbound.dto

data class SignupCommand(
    val registrationToken: String,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String?,
    /** 온보딩에서 선택한 관심 지역 법정동코드(10자리) 목록. 미선택이면 빈 리스트. */
    val areaCodes: List<String> = emptyList(),
)
