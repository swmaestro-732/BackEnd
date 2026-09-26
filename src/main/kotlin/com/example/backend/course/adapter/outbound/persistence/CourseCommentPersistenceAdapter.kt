package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseCommentRepository
import com.example.backend.course.application.port.outbound.CourseCommentPersistencePort
import com.example.backend.course.application.port.outbound.CourseCommentRow
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant

@Component
class CourseCommentPersistenceAdapter(
    private val courseCommentRepository: CourseCommentRepository,
) : CourseCommentPersistencePort {
    override fun save(
        courseId: Long,
        userId: Long,
        content: String,
    ): CourseCommentRow {
        val saved = courseCommentRepository.insert(courseId, userId, content)
        return CourseCommentRow(
            id = saved.id.value,
            authorId = saved.userId,
            content = saved.content,
            createdAt = saved.createdAt.toJavaInstant(),
            updatedAt = saved.updatedAt.toJavaInstant(),
        )
    }

    override fun updateContent(
        courseId: Long,
        commentId: Long,
        userId: Long,
        content: String,
    ): Int = courseCommentRepository.updateContent(courseId, commentId, userId, content)

    override fun softDelete(
        courseId: Long,
        commentId: Long,
        userId: Long,
    ): Int = courseCommentRepository.softDelete(courseId, commentId, userId)

    override fun findPage(
        courseId: Long,
        cursor: Long?,
        size: Int,
    ): List<CourseCommentRow> = courseCommentRepository.findPage(courseId, cursor, size)
}
