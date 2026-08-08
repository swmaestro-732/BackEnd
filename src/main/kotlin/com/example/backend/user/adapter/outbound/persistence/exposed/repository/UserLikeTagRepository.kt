package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.UserLikeTagTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/** user_like_tags 접근 리포지토리 — 사용자의 관심 태그를 전체 치환·조회한다. */
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

    /**
     * 사용자의 관심 태그 id 목록. 테이블 PK 가 (user_id, tag_id) 뿐이라 저장 순서를 복원할 수 없어
     * tag_id 오름차순으로 정렬한다(요청마다 순서가 흔들리지 않게).
     */
    fun findTagIdsByUserId(userId: Long): List<Long> =
        UserLikeTagTable
            .select(UserLikeTagTable.tagId)
            .where { UserLikeTagTable.userId eq userId }
            .orderBy(UserLikeTagTable.tagId)
            .map { it[UserLikeTagTable.tagId] }
}
