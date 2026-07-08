package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object FollowTable : Table("follows") {
    val id = long("id").autoIncrement()
    val followerId = long("follower_id")
    val followingId = long("following_id")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
