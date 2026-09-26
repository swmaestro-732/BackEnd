package com.example.backend.course.application.port.outbound

import java.time.Instant

data class CourseCommentRow(
    val id: Long,
    val authorId: Long,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

interface CourseCommentPersistencePort {
    fun save(
        courseId: Long,
        userId: Long,
        content: String,
    ): CourseCommentRow

    /** 해당 코스의 본인·미삭제 댓글만 갱신하고 영향받은 행 수를 반환한다. */
    fun updateContent(
        courseId: Long,
        commentId: Long,
        userId: Long,
        content: String,
    ): Int

    /** 해당 코스의 본인·미삭제 댓글만 소프트 삭제하고 영향받은 행 수를 반환한다. */
    fun softDelete(
        courseId: Long,
        commentId: Long,
        userId: Long,
    ): Int

    /** 미삭제 댓글을 id 내림차순으로 [cursor] 미만부터 최대 [size] + 1건 반환한다. */
    fun findPage(
        courseId: Long,
        cursor: Long?,
        size: Int,
    ): List<CourseCommentRow>
}
