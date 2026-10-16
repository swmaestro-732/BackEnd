package com.example.backend.course.application.port.inbound.dto

data class EditCourseCommentCommand(
    val courseId: Long,
    val commentId: Long,
    val userId: Long,
    val content: String,
)
