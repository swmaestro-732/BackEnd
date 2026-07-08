package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object TracingCourseTable : Table("tracing_courses") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val courseId = long("course_id") // cross-domain(course): FK 없음
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
