package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

data class CourseCommentResult(
    val id: Long,
    val authorId: Long,
    val authorNickname: String,
    val authorProfileImageUrl: String?,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isMine: Boolean,
)
