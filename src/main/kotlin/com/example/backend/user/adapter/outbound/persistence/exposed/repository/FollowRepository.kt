package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.FollowTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/** follows 테이블 접근 리포지토리 — 팔로우 관계의 삽입·삭제·조회만 담당한다(카운터 증감은 [UserRepository]). */
@Repository
class FollowRepository {
    /** 팔로우 관계를 삽입한다(이미 있으면 무시). 새로 심었으면 true. */
    fun insertIgnore(
        followerId: Long,
        followingId: Long,
    ): Boolean =
        FollowTable
            .insertIgnore {
                it[FollowTable.followerId] = followerId
                it[FollowTable.followingId] = followingId
            }.insertedCount > 0

    /** 팔로우 관계를 삭제한다. 실제로 지웠으면 true. */
    fun delete(
        followerId: Long,
        followingId: Long,
    ): Boolean =
        FollowTable.deleteWhere {
            (FollowTable.followerId eq followerId) and (FollowTable.followingId eq followingId)
        } > 0

    fun exists(
        followerId: Long,
        followingId: Long,
    ): Boolean =
        FollowTable
            .selectAll()
            .where {
                (FollowTable.followerId eq followerId) and (FollowTable.followingId eq followingId)
            }.empty()
            .not()

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
