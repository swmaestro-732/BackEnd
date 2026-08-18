package com.example.backend.course.adapter.inbound.web.response

/**
 * 웹 응답 DTO — 코스 리뷰 생성 결과. 생성된 리뷰 id 만 내려준다
 * (등록 후 화면은 "리뷰가 등록되었어요!"로 넘어가고, 목록은 다시 조회한다).
 */
data class CreateCourseReviewResponse(
    val reviewId: Long,
)
