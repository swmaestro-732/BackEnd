package com.example.backend.user.application.port.inbound.dto

/**
 * 소셜 로그인 결과. 로그인·가입을 하나로 합쳤으므로 항상 토큰을 발급한다.
 * [isNewUser] = 온보딩 미완료(handle 미설정) 마커 — true 면 클라이언트가 온보딩(PATCH /my/profile)으로 이동한다.
 */
data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
