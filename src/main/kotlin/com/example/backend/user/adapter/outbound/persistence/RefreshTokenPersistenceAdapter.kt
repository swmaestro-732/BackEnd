package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.bootstrap.security.JwtProperties
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenRecord
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Clock
import java.util.UUID
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/** 불투명 refresh token 을 발급하고 V4 refresh_tokens 테이블에 저장한다. */
@Repository
class RefreshTokenPersistenceAdapter(
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) : RefreshTokenPort {
    override fun issue(userId: Long): String {
        val token = UUID.randomUUID().toString()
        val expiresAt = clock.instant().plus(jwtProperties.refreshTtl).toKotlinInstant()
        RefreshTokenTable.insert {
            it[RefreshTokenTable.userId] = userId
            it[RefreshTokenTable.token] = token
            it[RefreshTokenTable.expiresAt] = expiresAt
        }
        return token
    }

    override fun find(token: String): RefreshTokenRecord? =
        RefreshTokenTable
            .selectAll()
            .where { RefreshTokenTable.token eq token }
            .singleOrNull()
            ?.let {
                RefreshTokenRecord(
                    id = it[RefreshTokenTable.id],
                    userId = it[RefreshTokenTable.userId],
                    token = it[RefreshTokenTable.token],
                    expiresAt = it[RefreshTokenTable.expiresAt].toJavaInstant(),
                    revoked = it[RefreshTokenTable.revoked],
                    createdAt = it[RefreshTokenTable.createdAt].toJavaInstant(),
                )
            }

    override fun validate(token: String): Boolean {
        val refreshToken = find(token) ?: return false
        return !refreshToken.revoked && refreshToken.expiresAt.isAfter(clock.instant())
    }

    override fun revoke(token: String) {
        RefreshTokenTable.update({ RefreshTokenTable.token eq token }) {
            it[RefreshTokenTable.revoked] = true
        }
    }
}
