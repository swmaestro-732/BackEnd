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
}
