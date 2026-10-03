package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseCommentPage

interface CourseCommentQueryUseCase {
    fun list(
        courseId: Long,
        viewerId: Long?,
        cursor: Long?,
        size: Int,
    ): CourseCommentPage
}
