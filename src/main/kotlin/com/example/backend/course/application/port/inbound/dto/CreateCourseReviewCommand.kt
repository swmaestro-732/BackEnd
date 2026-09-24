package com.example.backend.course.application.port.inbound.dto

import com.example.backend.course.domain.model.CourseReviewTag

/**
 * 인바운드 포트 입력 DTO — 코스 리뷰 작성.
 */
data class CreateCourseReviewCommand(
    val courseId: Long,
    val userId: Long,
    val rating: Int,
    val content: String?,
    val photoUrls: List<String>,
    val tags: Set<CourseReviewTag>,
)
