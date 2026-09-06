package com.example.backend.place.application.port.inbound.dto

import com.example.backend.place.domain.model.PlaceReviewTag

/**
 * 인바운드 포트 입력 DTO — 장소 리뷰 작성.
 */
data class CreatePlaceReviewCommand(
    val placeId: Long,
    val userId: Long,
    val rating: Int,
    val content: String?,
    val photoUrls: List<String>,
    val tags: Set<PlaceReviewTag>,
)
