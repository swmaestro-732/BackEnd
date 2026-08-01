package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.bootstrap.security.JwtProperties
import com.example.backend.user.adapter.outbound.persistence.exposed.repository.RefreshTokenRepository
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenRecord
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Clock
import java.util.UUID
import kotlin.time.toKotlinInstant

/**
 * 불투명 refresh token 을 발급하고, 저장/조회/폐기는 [RefreshTokenRepository] 에 위임한다.
 * 토큰 생성(UUID)·해시(SHA-256)·시각(clock) 등 표현 로직만 이 어댑터가 소유한다.
 */
@Component
class RefreshTokenPersistenceAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) : RefreshTokenPort {
    override fun issue(userId: Long): String {
        val token = UUID.randomUUID().toString()
        val expiresAt = clock.instant().plus(jwtProperties.refreshTtl).toKotlinInstant()
        refreshTokenRepository.insert(userId, hash(token), expiresAt)
        return token
    }

    override fun findValid(token: String): RefreshTokenRecord? =
        refreshTokenRepository.findValid(hash(token), clock.instant().toKotlinInstant())

    override fun revoke(token: String): Boolean = refreshTokenRepository.revoke(hash(token))

    override fun revokeAllByUser(userId: Long) = refreshTokenRepository.revokeAllByUser(userId)

    private fun hash(token: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
