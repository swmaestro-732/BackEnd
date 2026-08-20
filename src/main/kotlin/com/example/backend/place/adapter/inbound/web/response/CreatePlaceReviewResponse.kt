package com.example.backend.place.adapter.inbound.web.response

/**
 * 웹 응답 DTO — 장소 리뷰 생성 결과. 생성된 리뷰 id 만 내려준다
 * (등록 후 화면은 장소 상세/후기 목록으로 돌아가 다시 조회한다).
 */
data class CreatePlaceReviewResponse(
    val reviewId: Long,
)
