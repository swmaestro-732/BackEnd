package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.domain.model.CourseReviewTag
import org.jetbrains.exposed.v1.core.Table

// 태그 마스터(course_review_tags)는 V7 에서 없앴다 — 태그는 코드(enum) 정본이라 링크 테이블이 enum 이름을 직접 든다.
internal object CourseReviewTagLinkTable : Table("course_review_tag_links") {
    val courseReviewId = long("course_review_id")
    val tag = enumerationByName<CourseReviewTag>("tag", 32)
    override val primaryKey = PrimaryKey(courseReviewId, tag)
}
