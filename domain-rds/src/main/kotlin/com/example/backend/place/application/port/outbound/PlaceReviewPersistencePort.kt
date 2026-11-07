package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.PlaceReview

/** 아웃바운드 포트 — 장소 리뷰(place_reviews) 쓰기 계약.*/
interface PlaceReviewPersistencePort {
    /** 이 사용자가 해당 장소에 살아있는(미삭제) 리뷰를 이미 갖고 있는지. */
    fun existsActiveReview(
        placeId: Long,
        userId: Long,
    ): Boolean

    fun save(review: PlaceReview): PlaceReview

    fun softDelete(
        reviewId: Long,
        placeId: Long,
        userId: Long,
    ): Int
}
