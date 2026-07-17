package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.domain.model.CourseReviewStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object CourseReviewTable : Table("course_reviews") {
    val id = long("id").autoIncrement()
    val courseId = long("course_id") // 같은 도메인(course) 참조 — 스키마에 FK 있음
    val status = enumerationByName<CourseReviewStatus>("status", 32)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val userId = long("user_id") // cross-domain(user): FK 없음
    val rating = short("rating")
    val content = text("content").nullable()
    override val primaryKey = PrimaryKey(id)
}
