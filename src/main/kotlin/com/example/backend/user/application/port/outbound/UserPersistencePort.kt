package com.example.backend.user.application.port.outbound

import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User

/** 프로필 조회용 읽기 모델(카운터 캐시 포함). */
data class UserProfileRow(
    val id: Long,
    val nickname: String,
    val handle: String?,
    val profileImageUrl: String?,
    val bio: String?,
    val followersCnt: Int,
    val followingsCnt: Int,
    val publicCoursesCnt: Int,
    val followerCoursesCnt: Int,
    val privateCoursesCnt: Int,
)

/**
 * 아웃바운드 포트 — 애플리케이션이 영속성에 요구하는 계약.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface UserPersistencePort {
    fun findAll(): List<User>

    fun findById(id: Long): User?

    /**
     * 주어진 사용자들 중 활성(deleted_at IS NULL) 행을 id 오름차순으로 `SELECT … FOR UPDATE` 로 잠그고,
     * 실제로 잠근(=활성) id 집합을 반환한다. 탈퇴 정리와 사용자별 쓰기(팔로우·저장)를 같은 행 잠금으로 직렬화해
     * 탈퇴 도중 유입된 쓰기가 잔여 행·카운터 불일치를 남기는 것을 막는다. id 오름차순 잠금으로 데드락을 피한다.
     */
    fun lockActive(userIds: List<Long>): Set<Long>

    /** 핸들로 조회한다(deleted_at IS NULL, findById 와 동일한 소프트 삭제 시맨틱). 없으면 null. */
    fun findByHandle(handle: String): User?

    fun findProfile(userId: Long): UserProfileRow?

    fun findProfiles(userIds: List<Long>): List<UserProfileRow>

    /** 저장 후 식별자가 부여된 User 를 반환한다. */
    fun save(user: User): User

    fun update(user: User)

    /**
     * 작성자의 공개범위별 코스 개수 캐시를 증감한다(코스 발행/공개범위변경/삭제 시 호출).
     * 0 인 델타는 무시하고, 하나라도 0 이 아니면 단일 UPDATE 로 반영한다. 크로스 도메인 경계라 원시 int 만 받는다.
     */
    fun applyCourseCountDelta(
        userId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    )

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
