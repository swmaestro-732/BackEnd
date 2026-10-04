package com.example.backend.course.application.port.inbound.dto

data class CourseCommentPage(
    val items: List<CourseCommentResult>,
    val hasNext: Boolean,
)
