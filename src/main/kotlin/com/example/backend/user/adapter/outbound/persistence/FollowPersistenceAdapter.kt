package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.FollowRepository
import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserRepository
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [FollowPersistencePort] 를 구현한다.
 * follows 관계는 [FollowRepository], 팔로워/팔로잉 카운터 증감은 [UserRepository] 에 위임하고,
 * 이 어댑터는 관계 삽입·삭제 성사 여부에 맞춰 카운터 갱신을 조율한다.
 */
@Component
class FollowPersistenceAdapter(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
) : FollowPersistencePort {
    override fun follow(
        followerId: Long,
        followingId: Long,
    ): Boolean {
        val inserted = followRepository.insertIgnore(followerId, followingId)
        if (inserted) {
            userRepository.increaseFollowers(followingId)
            userRepository.increaseFollowings(followerId)
        }
        return inserted
    }

    override fun unfollow(
        followerId: Long,
        followingId: Long,
    ): Boolean {
        val deleted = followRepository.delete(followerId, followingId)
        if (deleted) {
            userRepository.decreaseFollowers(followingId)
            userRepository.decreaseFollowings(followerId)
        }
        return deleted
    }

    override fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean = followRepository.exists(followerId, followingId)

    override fun filterFollowing(
        followerId: Long,
        followingIds: List<Long>,
    ): Set<Long> = followRepository.filterFollowing(followerId, followingIds)

    override fun filterFollowers(
        followingId: Long,
        followerIds: List<Long>,
    ): Set<Long> = followRepository.filterFollowers(followingId, followerIds)
}
