package com.example.backend.course.application.port.outbound

import com.example.backend.course.domain.model.CourseReview

/** 아웃바운드 포트 — 코스 리뷰(course_reviews) 쓰기 계약.*/
interface CourseReviewPersistencePort {
    fun save(review: CourseReview): CourseReview

    fun softDelete(
        reviewId: Long,
        courseId: Long,
        userId: Long,
    ): Int
}
