package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.domain.model.CourseCommentStatus
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

internal object CourseCommentTable : LongIdTable("course_comments") {
    val courseId = long("course_id")
    val userId = long("user_id")
    val content = text("content")
    val status = enumerationByName<CourseCommentStatus>("status", 32)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
    val deletedAt = timestamp("deleted_at").nullable()
}

internal class CourseCommentEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<CourseCommentEntity>(CourseCommentTable)

    var courseId by CourseCommentTable.courseId
    var userId by CourseCommentTable.userId
    var content by CourseCommentTable.content
    var status by CourseCommentTable.status
    var createdAt by CourseCommentTable.createdAt
    var updatedAt by CourseCommentTable.updatedAt
    var deletedAt by CourseCommentTable.deletedAt
}
