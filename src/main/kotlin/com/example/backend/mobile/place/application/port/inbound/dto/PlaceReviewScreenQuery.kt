package com.example.backend.mobile.place.application.port.inbound.dto

import com.example.backend.place.application.port.inbound.dto.PlaceReviewSortKey

/** 인바운드 포트 입력 DTO — 후기 전체보기 화면 조합. */
data class PlaceReviewScreenQuery(
    val placeId: Long,
    val sort: PlaceReviewSortKey = PlaceReviewSortKey.LATEST,
    val descending: Boolean = true,
    val cursor: String? = null,
    val size: Int = 10,
)
