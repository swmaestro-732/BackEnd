package com.example.backend.user.application.port.outbound

import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User

/** 프로필 조회용 읽기 모델(카운터 캐시 포함). */
data class UserProfileRow(
    val id: Long,
    val nickname: String,
    val handle: String?,
    val profileImageUrl: String?,
    val followersCnt: Int,
    val followingsCnt: Int,
    val coursesCnt: Int,
)

/**
 * 아웃바운드 포트 — 애플리케이션이 영속성에 요구하는 계약.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface UserPersistencePort {
    fun findAll(): List<User>

    fun findById(id: Long): User?

    fun findProfile(userId: Long): UserProfileRow?

    /** 저장 후 식별자가 부여된 User 를 반환한다. */
    fun save(user: User): User

    fun update(user: User)

    fun softDelete(userId: Long)

    fun existsByHandle(handle: String): Boolean

    fun findBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User?

    /** 소셜 계정 정보와 함께 저장 후 식별자가 부여된 User 를 반환한다. */
    fun saveWithSocial(user: User): User
}
