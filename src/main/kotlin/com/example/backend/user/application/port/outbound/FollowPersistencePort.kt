package com.example.backend.user.application.port.outbound

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
}
