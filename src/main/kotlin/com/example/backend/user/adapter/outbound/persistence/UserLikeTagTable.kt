package com.example.backend.user.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object UserLikeTagTable : Table("user_like_tags") {
    val userId = long("user_id")
    val tagId = long("tag_id") // cross-domain(course.tags): FK 없음
    override val primaryKey = PrimaryKey(userId, tagId)
}
