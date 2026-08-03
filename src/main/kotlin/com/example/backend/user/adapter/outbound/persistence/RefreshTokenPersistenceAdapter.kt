package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.RefreshTokenRepository
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenRecord
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [RefreshTokenPort] 를 구현한다. 실제 테이블 접근은 [RefreshTokenRepository] 에 위임한다. */
@Component
class RefreshTokenPersistenceAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
) : RefreshTokenPort {
    override fun issue(userId: Long): String = refreshTokenRepository.issue(userId)

    override fun findValid(token: String): RefreshTokenRecord? = refreshTokenRepository.findValid(token)

    override fun revoke(token: String): Boolean = refreshTokenRepository.revoke(token)

    override fun revokeAllByUser(userId: Long) = refreshTokenRepository.revokeAllByUser(userId)
}
