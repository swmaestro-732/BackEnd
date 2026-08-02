package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.FollowRepository
import com.example.backend.user.application.port.outbound.FollowPersistencePort
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
}
