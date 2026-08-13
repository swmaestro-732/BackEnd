package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.FollowTable
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.application.port.outbound.FollowUserRow
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
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

    /**
     * 사용자의 팔로우 관계를 양방향으로 전부 제거하고 상대방 카운터를 보정한다(회원 탈퇴 정리).
     * - 내가 팔로우한 대상: 각 상대의 followers_cnt −1.
     * - 나를 팔로우한 사용자: 각 상대의 followings_cnt −1.
     * 내 자신의 카운터는 탈퇴 시 users 행이 함께 0으로 리셋되므로 여기서 건드리지 않는다.
     * (follows 는 (follower_id, following_id) 유니크라 각 상대는 최대 1행 → inList 일괄 −1 이 정확하다.)
     */
    fun purgeFollowsOf(userId: Long) {
        val followingIds =
            FollowTable
                .select(FollowTable.followingId)
                .where { FollowTable.followerId eq userId }
                .map { it[FollowTable.followingId] }
        val followerIds =
            FollowTable
                .select(FollowTable.followerId)
                .where { FollowTable.followingId eq userId }
                .map { it[FollowTable.followerId] }

        if (followingIds.isNotEmpty()) {
            UserTable.update({ UserTable.id inList followingIds }) {
                it[followersCnt] = followersCnt - 1
            }
        }
        if (followerIds.isNotEmpty()) {
            UserTable.update({ UserTable.id inList followerIds }) {
                it[followingsCnt] = followingsCnt - 1
            }
        }

        FollowTable.deleteWhere {
            (FollowTable.followerId eq userId) or (FollowTable.followingId eq userId)
        }
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
                    userId = it[UserTable.id].value,
                    nickname = it[UserTable.nickname],
                    handle = it[UserTable.handle],
                    profileImageUrl = it[UserTable.profileImageUrl],
                )
            }
}
