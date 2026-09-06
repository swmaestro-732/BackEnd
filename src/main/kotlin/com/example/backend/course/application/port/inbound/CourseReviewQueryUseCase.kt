package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseReviewsQuery
import com.example.backend.course.application.port.inbound.dto.CourseReviewsResult

/** 인바운드 포트 — 코스 리뷰 목록 조회(공개 API). */
interface CourseReviewQueryUseCase {
    fun getReviews(query: CourseReviewsQuery): CourseReviewsResult
}
