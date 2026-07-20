package com.example.backend.user.application.port.outbound

import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User

/**
 * 아웃바운드 포트 — 애플리케이션이 영속성에 요구하는 계약.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface UserPersistencePort {
    fun findAll(): List<User>

    /** 저장 후 식별자가 부여된 User 를 반환한다. */
    fun save(user: User): User

    fun findBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User?

    /** 소셜 계정 정보와 함께 저장 후 식별자가 부여된 User 를 반환한다. */
    fun saveWithSocial(user: User): User
}
