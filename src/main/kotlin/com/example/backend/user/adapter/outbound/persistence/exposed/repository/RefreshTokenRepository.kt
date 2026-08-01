package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.RefreshTokenTable
import com.example.backend.user.application.port.outbound.RefreshTokenRecord
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** refresh_tokens 테이블 접근 리포지토리 — 불투명 토큰 해시의 저장·조회·폐기만 담당한다(토큰 발급·해시는 어댑터). */
@Repository
class RefreshTokenRepository {
    fun insert(
        userId: Long,
        tokenHash: String,
        expiresAt: Instant,
    ) {
        RefreshTokenTable.insert {
            it[RefreshTokenTable.userId] = userId
            it[RefreshTokenTable.tokenHash] = tokenHash
            it[RefreshTokenTable.expiresAt] = expiresAt
        }
    }

    /** 폐기되지 않고 만료(now 기준) 전인 토큰 레코드를 해시로 조회한다. */
    fun findValid(
        tokenHash: String,
        now: Instant,
    ): RefreshTokenRecord? =
        RefreshTokenTable
            .selectAll()
            .where {
                (RefreshTokenTable.tokenHash eq tokenHash) and
                    (RefreshTokenTable.revoked eq false) and
                    (RefreshTokenTable.expiresAt greater now)
            }.singleOrNull()
            ?.let {
                RefreshTokenRecord(
                    id = it[RefreshTokenTable.id],
                    userId = it[RefreshTokenTable.userId],
                    tokenHash = it[RefreshTokenTable.tokenHash],
                    expiresAt = it[RefreshTokenTable.expiresAt].toJavaInstant(),
                    revoked = it[RefreshTokenTable.revoked],
                    createdAt = it[RefreshTokenTable.createdAt].toJavaInstant(),
                )
            }

    fun revoke(tokenHash: String): Boolean =
        RefreshTokenTable.update({
            (RefreshTokenTable.tokenHash eq tokenHash) and (RefreshTokenTable.revoked eq false)
        }) {
            it[revoked] = true
        } > 0

    fun revokeAllByUser(userId: Long) {
        RefreshTokenTable.update({
            (RefreshTokenTable.userId eq userId) and (RefreshTokenTable.revoked eq false)
        }) {
            it[revoked] = true
        }
    }
}
