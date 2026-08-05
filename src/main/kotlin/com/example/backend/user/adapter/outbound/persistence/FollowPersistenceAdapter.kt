package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.FollowRepository
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.FollowUserRow
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [FollowPersistencePort] 를 구현한다. 실제 테이블 접근은 [FollowRepository] 에 위임한다. */
@Component
class FollowPersistenceAdapter(
    private val followRepository: FollowRepository,
) : FollowPersistencePort {
    override fun follow(
        followerId: Long,
        followingId: Long,
    ): Boolean = followRepository.follow(followerId, followingId)

    override fun unfollow(
        followerId: Long,
        followingId: Long,
    ): Boolean = followRepository.unfollow(followerId, followingId)

    override fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean = followRepository.isFollowing(followerId, followingId)

    override fun filterFollowing(
        followerId: Long,
        followingIds: List<Long>,
    ): Set<Long> = followRepository.filterFollowing(followerId, followingIds)

    override fun filterFollowers(
        followingId: Long,
        followerIds: List<Long>,
    ): Set<Long> = followRepository.filterFollowers(followingId, followerIds)

    override fun findFollowers(
        targetUserId: Long,
        cursorId: Long?,
        limit: Int,
    ): List<FollowUserRow> = followRepository.findFollowers(targetUserId, cursorId, limit)

    override fun findFollowings(
        targetUserId: Long,
        cursorId: Long?,
        limit: Int,
    ): List<FollowUserRow> = followRepository.findFollowings(targetUserId, cursorId, limit)
}
