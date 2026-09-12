package com.example.backend.user.application.port.outbound

import com.example.backend.user.domain.model.Identity
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User

/**
 * 아웃바운드 포트 — identity/oauth_credential 영속성 계약(SCRUM-466 계정 분리).
 * 소셜 자격증명으로 프로필을 찾거나, 신규 가입 시 identity·자격증명·기본 User 를 함께 만든다.
 */
interface IdentityPersistencePort {
    /** 소셜 자격증명에 매인 활성 기본 프로필(User)을 조회한다(deleted_at IS NULL). 없으면 null. */
    fun findActiveUserByCredential(
        provider: SocialProvider,
        socialId: String,
    ): User?

    /** 소셜 자격증명에 매인 탈퇴 기본 프로필(User)을 조회한다(deleted_at IS NOT NULL). 없으면 null. */
    fun findWithdrawnUserByCredential(
        provider: SocialProvider,
        socialId: String,
    ): User?

    /**
     * 신규 가입 — identity 와 그 자격증명들, 기본 프로필 User 를 한 트랜잭션에서 만들고
     * 식별자가 부여된 User 를 반환한다.
     */
    fun register(
        identity: Identity,
        primaryUser: User,
    ): User
}
