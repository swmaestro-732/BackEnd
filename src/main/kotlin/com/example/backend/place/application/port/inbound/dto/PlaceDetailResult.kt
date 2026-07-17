package com.example.backend.place.application.port.inbound.dto

import java.time.Instant

/** 장소 상세 유스케이스 결과 — 노션 명세(Place · 장소 상세)의 data.place에 대응. */
data class PlaceDetailResult(
    val id: Long,
    val name: String,
    val categories: List<String>,
    val imageUrls: List<String>,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val openStatus: OpenStatus,
    val openingHoursText: String?,
    val reviewSummary: ReviewSummary,
    val viewerHasSaved: Boolean,
) {
    /** 응답 계약상 영업 상태 — 응답은 String 직렬화(api-design 정책). */
    enum class OpenStatus { OPEN, CLOSED }

    data class ReviewSummary(
        val averageRating: Double,
        val totalCount: Int,
        val reviews: List<Review>,
    )

    data class Review(
        val id: Long,
        val authorId: Long,
        val authorNickname: String,
        val authorProfileImageUrl: String?,
        val rating: Int,
        val content: String,
        val createdAt: Instant,
        val relativeTime: String,
        val photoUrls: List<String>,
    )
}
