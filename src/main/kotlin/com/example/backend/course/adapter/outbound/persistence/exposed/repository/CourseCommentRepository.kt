package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseCommentEntity
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseCommentTable
import com.example.backend.course.application.port.outbound.CourseCommentRow
import com.example.backend.course.domain.model.CourseCommentStatus
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

@Repository
class CourseCommentRepository {
    internal fun insert(
        courseId: Long,
        userId: Long,
        content: String,
    ): CourseCommentEntity =
        CourseCommentEntity
            .new {
                this.courseId = courseId
                this.userId = userId
                this.content = content
                status = CourseCommentStatus.ACTIVE
            }.also { it.refresh(flush = true) }

    fun updateContent(
        courseId: Long,
        commentId: Long,
        userId: Long,
        content: String,
    ): Int =
        CourseCommentTable.update({
            (CourseCommentTable.courseId eq courseId) and
                (CourseCommentTable.id eq commentId) and
                (CourseCommentTable.userId eq userId) and
                CourseCommentTable.deletedAt.isNull()
        }) {
            it[CourseCommentTable.content] = content
            it[updatedAt] = Clock.System.now()
        }

    fun softDelete(
        courseId: Long,
        commentId: Long,
        userId: Long,
    ): Int {
        val now = Clock.System.now()
        return CourseCommentTable.update({
            (CourseCommentTable.courseId eq courseId) and
                (CourseCommentTable.id eq commentId) and
                (CourseCommentTable.userId eq userId) and
                CourseCommentTable.deletedAt.isNull()
        }) {
            it[deletedAt] = now
            it[updatedAt] = now
            it[status] = CourseCommentStatus.DELETED
        }
    }

    fun findPage(
        courseId: Long,
        cursor: Long?,
        size: Int,
    ): List<CourseCommentRow> {
        var condition = (CourseCommentTable.courseId eq courseId) and CourseCommentTable.deletedAt.isNull()
        cursor?.let { condition = condition and (CourseCommentTable.id less it) }
        return CourseCommentTable
            .selectAll()
            .where(condition)
            .orderBy(CourseCommentTable.id to SortOrder.DESC)
            .limit(size + 1)
            .map {
                CourseCommentRow(
                    id = it[CourseCommentTable.id].value,
                    authorId = it[CourseCommentTable.userId],
                    content = it[CourseCommentTable.content],
                    createdAt = it[CourseCommentTable.createdAt].toJavaInstant(),
                    updatedAt = it[CourseCommentTable.updatedAt].toJavaInstant(),
                )
            }
    }
}
