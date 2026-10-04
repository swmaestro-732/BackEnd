package com.example.backend.course.application.port.outbound

import com.example.backend.course.domain.model.CourseReview

/** 아웃바운드 포트 — 코스 리뷰(course_reviews) 쓰기 계약.*/
interface CourseReviewPersistencePort {
    /** 이 사용자가 해당 코스에 살아있는(미삭제) 리뷰를 이미 갖고 있는지. */
    fun existsActiveReview(
        courseId: Long,
        userId: Long,
    ): Boolean

    fun save(review: CourseReview): CourseReview

    fun softDelete(
        reviewId: Long,
        courseId: Long,
        userId: Long,
    ): Int
}
