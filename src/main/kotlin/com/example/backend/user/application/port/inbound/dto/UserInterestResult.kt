package com.example.backend.user.application.port.inbound.dto

/** 유스케이스 출력 — 사용자의 관심 테마 이름과 관심 지역 법정동코드. */
data class UserInterestResult(
    val likeThemes: List<String>,
    val areaCodes: List<String>,
)
