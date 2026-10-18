package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.application.port.inbound.dto.CreateCourseCommentCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCourseCommentRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
) {
    fun toCommand(
        userId: Long,
        courseId: Long,
    ): CreateCourseCommentCommand = CreateCourseCommentCommand(courseId, userId, content)
}
