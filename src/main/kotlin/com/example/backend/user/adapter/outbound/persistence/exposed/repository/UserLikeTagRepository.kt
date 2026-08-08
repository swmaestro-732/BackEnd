package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.UserLikeTagTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.springframework.stereotype.Repository

/** user_like_tags 접근 리포지토리 — 사용자의 관심 태그를 전체 치환한다. */
@Repository
class UserLikeTagRepository {
    fun replaceLikeTags(
        userId: Long,
        tagIds: List<Long>,
    ) {
        UserLikeTagTable.deleteWhere { UserLikeTagTable.userId eq userId }
        val distinct = tagIds.distinct()
        if (distinct.isEmpty()) return
        UserLikeTagTable.batchInsert(distinct, ignore = true) { tagId ->
            this[UserLikeTagTable.userId] = userId
            this[UserLikeTagTable.tagId] = tagId
        }
    }
}
