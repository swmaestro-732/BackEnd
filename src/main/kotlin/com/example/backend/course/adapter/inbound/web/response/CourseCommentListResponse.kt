package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseCommentPage

data class CourseCommentListResponse(
    val items: List<CourseCommentResponse>,
    val hasNext: Boolean,
) {
    companion object {
        fun from(page: CourseCommentPage): CourseCommentListResponse =
            CourseCommentListResponse(
                items = page.items.map(CourseCommentResponse::from),
                hasNext = page.hasNext,
            )
    }
}
