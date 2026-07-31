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

    fun findProfiles(userIds: List<Long>): List<UserProfileRow>

    /** 저장 후 식별자가 부여된 User 를 반환한다. */
    fun save(user: User): User

    fun update(user: User)

    fun softDelete(user: User)

    fun existsByNickname(nickname: String): Boolean

    fun existsByHandle(handle: String): Boolean

    fun findBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User?

    /** 탈퇴(soft delete, deletedAt IS NOT NULL)한 소셜 계정 행을 조회한다. */
    fun findWithdrawnBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User?

    /** 지정한 사용자를 제외하고 닉네임 중복 여부를 검사한다(재활성화 시 자기 자신 제외). */
    fun existsByNicknameExcludingUser(
        nickname: String,
        excludeUserId: Long,
    ): Boolean

    /** 지정한 사용자를 제외하고 핸들 중복 여부를 검사한다(재활성화 시 자기 자신 제외). */
    fun existsByHandleExcludingUser(
        handle: String,
        excludeUserId: Long,
    ): Boolean

    /** 소셜 계정 정보와 함께 저장 후 식별자가 부여된 User 를 반환한다. */
    fun saveWithSocial(user: User): User

    /** 탈퇴 행을 재활성화(status=ACTIVE, deletedAt=NULL, 프로필 갱신)하고 복원된 User 를 반환한다. */
    fun reactivate(user: User): User
}
