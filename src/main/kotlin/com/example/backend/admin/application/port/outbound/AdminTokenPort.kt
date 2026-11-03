package com.example.backend.admin.application.port.outbound

import java.time.Instant

/** 어드민 전용 토큰 발급 포트. 앱 access token 과 purpose 가 달라 서로 호환되지 않는다. */
interface AdminTokenPort {
    fun issue(email: String): IssuedAdminToken
}

data class IssuedAdminToken(
    val token: String,
    val expiresAt: Instant,
) {
    override fun toString(): String = "IssuedAdminToken(token=[REDACTED], expiresAt=$expiresAt)"
}
