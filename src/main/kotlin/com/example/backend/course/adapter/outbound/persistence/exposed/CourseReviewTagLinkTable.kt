package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table

internal object CourseReviewTagLinkTable : Table("course_review_tag_links") {
    val courseReviewId = long("course_review_id")
    val courseReviewTagId = long("course_review_tag_id")
    override val primaryKey = PrimaryKey(courseReviewId, courseReviewTagId)
}
