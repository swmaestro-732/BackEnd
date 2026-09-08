package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CreateCourseReviewCommand
import com.example.backend.course.domain.model.CourseReview

/** 인바운드 포트 — 코스 리뷰 작성·삭제(공개 API). */
interface CourseReviewUseCase {
    fun create(command: CreateCourseReviewCommand): CourseReview

    fun delete(
        userId: Long,
        courseId: Long,
        reviewId: Long,
    )
}
