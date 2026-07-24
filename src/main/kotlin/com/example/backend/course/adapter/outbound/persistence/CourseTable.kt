package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

internal object CourseTable : Table("courses") {
    val id = long("id").autoIncrement()
    val status = enumerationByName<CourseStatus>("status", 32)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val userId = long("user_id") // cross-domain(user): FK 없음
    val title = varchar("title", 200)
    val coverImageUrl = text("cover_image_url").nullable()
    val description = text("description").nullable()
    val category = enumerationByName<CourseCategory>("category", 50).nullable()
    val area = varchar("area", 100).nullable()
    val visitDate = date("visit_date").nullable()
    val isPublished = bool("is_published")
    val visibility = enumerationByName<CourseVisibility>("visibility", 32)
    val likesCnt = integer("likes_cnt")
    val commentsCnt = integer("comments_cnt")
    val savesCnt = integer("saves_cnt")
    val tracingsCnt = integer("tracings_cnt")
    val forkedFromId = long("forked_from_id").nullable() // 포크 원본 course (같은 도메인)
    override val primaryKey = PrimaryKey(id)
}
