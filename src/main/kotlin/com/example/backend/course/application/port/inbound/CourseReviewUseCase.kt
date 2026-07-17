package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseReviewPageResult
import com.example.backend.course.application.port.inbound.dto.CourseReviewQuery

/**
 * 인바운드 포트 — 코스 리뷰 목록 조회(정렬·페이지네이션).
 * 코스 상세([CourseUseCase])와 책임을 분리해, 리뷰가 필요한 다른 화면에서도 재사용한다.
 */
interface CourseReviewUseCase {
    fun getReviews(
        courseId: Long,
        query: CourseReviewQuery,
    ): CourseReviewPageResult
}
