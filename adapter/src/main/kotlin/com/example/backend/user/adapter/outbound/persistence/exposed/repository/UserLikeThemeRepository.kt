package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.UserLikeCategoryTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/** user_like_categories 접근 리포지토리 — 사용자의 관심 테마(코스 카테고리 이름)를 전체 치환·조회한다. */
@Repository
class UserLikeThemeRepository {
    fun replaceLikeThemes(
        userId: Long,
        themes: List<String>,
    ) {
        UserLikeCategoryTable.deleteWhere { UserLikeCategoryTable.userId eq userId }
        val distinct = themes.distinct()
        if (distinct.isEmpty()) return
        UserLikeCategoryTable.batchInsert(distinct, ignore = true) { theme ->
            this[UserLikeCategoryTable.userId] = userId
            this[UserLikeCategoryTable.category] = theme
        }
    }

    /**
     * 사용자의 관심 테마 목록. PK 가 (user_id, category) 뿐이라 저장 순서를 복원할 수 없어
     * 이름 오름차순으로 정렬한다(요청마다 순서가 흔들리지 않게).
     */
    fun findThemesByUserId(userId: Long): List<String> =
        UserLikeCategoryTable
            .select(UserLikeCategoryTable.category)
            .where { UserLikeCategoryTable.userId eq userId }
            .orderBy(UserLikeCategoryTable.category)
            .map { it[UserLikeCategoryTable.category] }
}
