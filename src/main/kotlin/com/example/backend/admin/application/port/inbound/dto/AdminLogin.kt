package com.example.backend.admin.application.port.inbound.dto

import java.time.Instant

/** 어드민 로그인 결과. 토큰은 로그에 남지 않게 toString 에서 가린다. */
data class AdminLogin(
    val accessToken: String,
    val expiresAt: Instant,
    val email: String,
) {
    override fun toString(): String = "AdminLogin(accessToken=[REDACTED], expiresAt=$expiresAt, email=$email)"
}
