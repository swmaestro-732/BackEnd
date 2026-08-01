package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.FollowTable
import com.example.backend.user.adapter.outbound.persistence.UserTable
import com.example.backend.user.application.port.outbound.FollowUserRow
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
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

    /** targetUserId 를 팔로우하는 사용자(팔로워, follower_id) 목록. 상대 컬럼은 following_id 로 고정. */
    fun findFollowers(
        targetUserId: Long,
        cursorId: Long?,
        limit: Int,
    ): List<FollowUserRow> =
        findPage(
            pivotColumn = FollowTable.followingId,
            userColumn = FollowTable.followerId,
            pivotUserId = targetUserId,
            cursorId = cursorId,
            limit = limit,
        )

    /** targetUserId 가 팔로우하는 사용자(팔로잉, following_id) 목록. 상대 컬럼은 follower_id 로 고정. */
    fun findFollowings(
        targetUserId: Long,
        cursorId: Long?,
        limit: Int,
    ): List<FollowUserRow> =
        findPage(
            pivotColumn = FollowTable.followerId,
            userColumn = FollowTable.followingId,
            pivotUserId = targetUserId,
            cursorId = cursorId,
            limit = limit,
        )

    /**
     * follows ⋈ users 이너 조인 한 쿼리로 (follows.id + 사용자 요약)을 최신 팔로우순(follows.id DESC)으로 페이지 조회한다.
     * - pivotColumn: 조회 기준이 되는 쪽(팔로워 목록이면 following_id, 팔로잉 목록이면 follower_id).
     * - userColumn: 목록에 실릴 사용자를 가리키는 쪽(users 와 조인).
     * 탈퇴자(users.deleted_at NOT NULL)는 제외한다.
     */
    private fun findPage(
        pivotColumn: Column<Long>,
        userColumn: Column<Long>,
        pivotUserId: Long,
        cursorId: Long?,
        limit: Int,
    ): List<FollowUserRow> =
        FollowTable
            .join(UserTable, JoinType.INNER, userColumn, UserTable.id)
            .select(FollowTable.id, UserTable.id, UserTable.nickname, UserTable.handle, UserTable.profileImageUrl)
            .where {
                var condition = (pivotColumn eq pivotUserId) and UserTable.deletedAt.isNull()
                cursorId?.let { condition = condition and (FollowTable.id less it) }
                condition
            }.orderBy(FollowTable.id to SortOrder.DESC)
            .limit(limit)
            .map {
                FollowUserRow(
                    followId = it[FollowTable.id],
                    userId = it[UserTable.id],
                    nickname = it[UserTable.nickname],
                    handle = it[UserTable.handle],
                    profileImageUrl = it[UserTable.profileImageUrl],
                )
            }
}
