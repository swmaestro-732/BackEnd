package com.example.backend.user.application.port.outbound

import java.time.Instant

/** DB에 저장된 불투명 refresh token. */
data class RefreshTokenRecord(
    val id: Long,
    val userId: Long,
    val token: String,
    val expiresAt: Instant,
    val revoked: Boolean,
    val createdAt: Instant,
)

/** refresh token 발급·검증·폐기 포트. */
interface RefreshTokenPort {
    fun issue(userId: Long): String

    /** 존재하고 폐기되지 않았으며 만료되지 않은 토큰만 반환한다. */
    fun findValid(token: String): RefreshTokenRecord?

    /** 실제로 폐기된 행이 있으면 true, 이미 폐기됐거나 없으면 false. */
    fun revoke(token: String): Boolean
}
