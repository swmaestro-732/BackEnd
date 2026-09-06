package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.domain.model.CourseReview

/**
 * 웹 응답 DTO — 코스 리뷰 생성 결과. 생성된 리뷰 id 만 내려준다
 * (등록 후 화면은 "리뷰가 등록되었어요!"로 넘어가고, 목록은 다시 조회한다).
 */
data class CreateCourseReviewResponse(
    val reviewId: Long,
) {
    companion object {
        /**
         * 저장된 리뷰([CourseReview])를 응답으로 옮긴다. 도메인의 id 는 insert 전 미정이라 nullable 이지만
         * 저장을 마친 리뷰에는 항상 채워져 있으므로 여기서 한 번만 확정한다.
         */
        fun from(review: CourseReview) = CreateCourseReviewResponse(reviewId = requireNotNull(review.id))

        /** 생성 `?mock=true` 폴백 응답 — 후기 목록 목 데이터(리뷰 1~6) 다음 번호. 실구현 목이 사라질 때 함께 제거한다. */
        val MOCK = CreateCourseReviewResponse(reviewId = 7L)
    }
}
