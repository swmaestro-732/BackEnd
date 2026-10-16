package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseCommentResult
import java.time.Instant

data class CourseCommentResponse(
    val id: Long,
    val authorId: Long,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isMine: Boolean,
) {
    companion object {
        fun from(result: CourseCommentResult): CourseCommentResponse =
            CourseCommentResponse(
                id = result.id,
                authorId = result.authorId,
                content = result.content,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt,
                isMine = result.isMine,
            )
    }
}
