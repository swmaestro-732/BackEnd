package com.example.backend.place.application.port.inbound

import com.example.backend.place.application.port.inbound.dto.CreatePlaceReviewCommand
import com.example.backend.place.domain.model.PlaceReview

/** 인바운드 포트 — 장소 리뷰 작성·삭제(공개 API). */
interface PlaceReviewUseCase {
    fun create(command: CreatePlaceReviewCommand): PlaceReview

    fun delete(
        userId: Long,
        placeId: Long,
        reviewId: Long,
    )
}
