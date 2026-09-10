package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.IdentityRepository
import com.example.backend.user.application.port.outbound.IdentityPersistencePort
import com.example.backend.user.domain.model.Identity
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [IdentityPersistencePort] 를 구현한다.
 * 실제 테이블 접근·도메인 매핑은 [IdentityRepository] 에 위임하고, 이 어댑터는 유스케이스 계약만 맞춘다.
 */
@Component
class IdentityPersistenceAdapter(
    private val identityRepository: IdentityRepository,
) : IdentityPersistencePort {
    override fun findActiveUserByCredential(
        provider: SocialProvider,
        socialId: String,
    ): User? = identityRepository.findActiveUserByCredential(provider, socialId)

    override fun findWithdrawnUserByCredential(
        provider: SocialProvider,
        socialId: String,
    ): User? = identityRepository.findWithdrawnUserByCredential(provider, socialId)

    override fun register(
        identity: Identity,
        primaryUser: User,
    ): User = identityRepository.register(identity, primaryUser)
}
