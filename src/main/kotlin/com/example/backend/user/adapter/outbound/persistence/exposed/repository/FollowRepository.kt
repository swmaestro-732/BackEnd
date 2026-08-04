package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.FollowTable
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

/** follows 테이블 접근 리포지토리 — 팔로우 관계 생성·해제와 users 의 팔로워/팔로잉 카운터 반영을 담당한다. */
@Repository
class FollowRepository {
    fun follow(
        followerId: Long,
        followingId: Long,
    ): Boolean {
        val inserted =
            FollowTable
                .insertIgnore {
                    it[FollowTable.followerId] = followerId
                    it[FollowTable.followingId] = followingId
                }.insertedCount > 0

        if (inserted) {
            UserTable.update({ UserTable.id eq followingId }) {
                it[followersCnt] = followersCnt + 1
            }
            UserTable.update({ UserTable.id eq followerId }) {
                it[followingsCnt] = followingsCnt + 1
            }
        }
        return inserted
    }

    fun unfollow(
        followerId: Long,
        followingId: Long,
    ): Boolean {
        val deleted =
            FollowTable.deleteWhere {
                (FollowTable.followerId eq followerId) and (FollowTable.followingId eq followingId)
            } > 0

        if (deleted) {
            UserTable.update({ UserTable.id eq followingId }) {
                it[followersCnt] = followersCnt - 1
            }
            UserTable.update({ UserTable.id eq followerId }) {
                it[followingsCnt] = followingsCnt - 1
            }
        }
        return deleted
    }

    fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean =
        FollowTable
            .selectAll()
            .where {
                (FollowTable.followerId eq followerId) and (FollowTable.followingId eq followingId)
            }.empty()
            .not()

    /** followerId 가 팔로우하는 대상 중 followingIds 에 속한 id 집합(조회자 기준 배치 조회). */
    fun filterFollowing(
        followerId: Long,
        followingIds: List<Long>,
    ): Set<Long> {
        if (followingIds.isEmpty()) return emptySet()
        return FollowTable
            .selectAll()
            .where {
                (FollowTable.followerId eq followerId) and (FollowTable.followingId inList followingIds)
            }.mapTo(mutableSetOf()) { it[FollowTable.followingId] }
    }

    /** followerIds 중 followingId 를 팔로우하는 id 집합(조회자 기준 배치 조회). */
    fun filterFollowers(
        followingId: Long,
        followerIds: List<Long>,
    ): Set<Long> {
        if (followerIds.isEmpty()) return emptySet()
        return FollowTable
            .selectAll()
            .where {
                (FollowTable.followingId eq followingId) and (FollowTable.followerId inList followerIds)
            }.mapTo(mutableSetOf()) { it[FollowTable.followerId] }
    }
}
