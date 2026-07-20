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

/** refresh token 저장·조회 포트. 검증과 폐기는 Stage 3에서 사용한다. */
interface RefreshTokenPort {
    fun issue(userId: Long): String

    fun find(token: String): RefreshTokenRecord?

    fun validate(token: String): Boolean

    fun revoke(token: String)
}
