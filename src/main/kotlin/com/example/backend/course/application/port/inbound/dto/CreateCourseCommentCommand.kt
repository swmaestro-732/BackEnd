package com.example.backend.course.application.port.inbound.dto

data class CreateCourseCommentCommand(
    val courseId: Long,
    val userId: Long,
    val content: String,
)
