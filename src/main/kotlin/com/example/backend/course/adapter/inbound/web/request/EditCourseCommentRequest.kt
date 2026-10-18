package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.application.port.inbound.dto.EditCourseCommentCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class EditCourseCommentRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
) {
    fun toCommand(
        userId: Long,
        courseId: Long,
        commentId: Long,
    ): EditCourseCommentCommand = EditCourseCommentCommand(courseId, commentId, userId, content)
}
