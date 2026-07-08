package com.example.backend.course.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object CourseTagTable : Table("course_tags") {
    val courseId = long("course_id")
    val tagId = long("tag_id")
    override val primaryKey = PrimaryKey(courseId, tagId)
}
