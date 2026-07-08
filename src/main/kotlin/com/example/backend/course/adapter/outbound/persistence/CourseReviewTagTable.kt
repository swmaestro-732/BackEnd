package com.example.backend.course.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object CourseReviewTagTable : Table("course_review_tags") {
    val id = long("id").autoIncrement()
    val label = varchar("label", 20)
    val icon = varchar("icon", 20)
    override val primaryKey = PrimaryKey(id)
}
