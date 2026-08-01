package com.example.backend.user.application.port.outbound

/** 팔로워/팔로잉 목록 항목용 읽기 모델 — follows 레코드 id(커서용) + 사용자 요약. */
data class FollowUserRow(
    // follows 레코드 id — 커서 페이지네이션 기준(사용자 id 아님).
    val followId: Long,
    val userId: Long,
    val nickname: String,
    val handle: String?,
    val profileImageUrl: String?,
)

interface FollowPersistencePort {
    /** 팔로우(멱등). 실제로 새로 생성됐으면 true. */
    fun follow(
        followerId: Long,
        followingId: Long,
    ): Boolean

    /** 언팔로우. 실제로 삭제됐으면 true. */
    fun unfollow(
        followerId: Long,
        followingId: Long,
    ): Boolean

    fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean

    /** followingIds 중 followerId 가 팔로우하는 대상만 골라 반환한다(배치 isFollowing, N+1 회피). */
    fun filterFollowing(
        followerId: Long,
        followingIds: List<Long>,
    ): Set<Long>

    /** followerIds 중 followingId 를 팔로우하는 사용자만 골라 반환한다(배치 isFollower, N+1 회피). */
    fun filterFollowers(
        followingId: Long,
        followerIds: List<Long>,
    ): Set<Long>

    /** targetUserId 를 팔로우하는 사용자(팔로워) 목록을 follows.id 내림차순으로 커서 페이지 조회한다. */
    fun findFollowers(
        targetUserId: Long,
        cursorId: Long?,
        limit: Int,
    ): List<FollowUserRow>

    /** targetUserId 가 팔로우하는 사용자(팔로잉) 목록을 follows.id 내림차순으로 커서 페이지 조회한다. */
    fun findFollowings(
        targetUserId: Long,
        cursorId: Long?,
        limit: Int,
    ): List<FollowUserRow>
}
