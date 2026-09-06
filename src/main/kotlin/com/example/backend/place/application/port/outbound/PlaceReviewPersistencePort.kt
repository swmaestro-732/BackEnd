package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.PlaceReview

/** 아웃바운드 포트 — 장소 리뷰(place_reviews) 쓰기 계약.*/
interface PlaceReviewPersistencePort {
    fun save(review: PlaceReview): PlaceReview

    fun softDelete(
        reviewId: Long,
        placeId: Long,
        userId: Long,
    ): Int
}
