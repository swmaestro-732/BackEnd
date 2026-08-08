package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table

/**
 * 관심 테마 매핑. 값은 course 도메인 `CourseCategory` enum 이름이다(V15 로 tag_id → category 전환).
 * 크로스 도메인 참조라 FK 없이 이름 문자열만 저장하며, 유효성은 course 인바운드 포트로 검증한다.
 */
internal object UserLikeThemeTable : Table("user_like_tags") {
    val userId = long("user_id")
    val category = varchar("category", 20)
    override val primaryKey = PrimaryKey(userId, category)
}
