package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.outbound.FollowPersistencePort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class FollowPersistenceAdapter : FollowPersistencePort {
    override fun follow(
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

    override fun unfollow(
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

    override fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean =
        FollowTable
            .selectAll()
            .where {
                (FollowTable.followerId eq followerId) and (FollowTable.followingId eq followingId)
            }.empty()
            .not()
}
