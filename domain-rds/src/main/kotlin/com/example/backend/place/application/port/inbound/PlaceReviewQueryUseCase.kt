package com.example.backend.place.application.port.inbound

import com.example.backend.place.application.port.inbound.dto.PlaceReviewsQuery
import com.example.backend.place.application.port.inbound.dto.PlaceReviewsResult

/** 인바운드 포트 — 장소 리뷰 목록 조회(공개 API). */
interface PlaceReviewQueryUseCase {
    fun getReviews(query: PlaceReviewsQuery): PlaceReviewsResult
}
