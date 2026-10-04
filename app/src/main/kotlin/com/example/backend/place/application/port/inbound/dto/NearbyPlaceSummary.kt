package com.example.backend.place.application.port.inbound.dto

/**
 * 인바운드 포트 반환 DTO — 기준점에서 가까운 장소 요약(홈 근처 저장 장소 카드용).
 *
 * - [rating] 소수 첫째 자리 평균 별점(리뷰 없으면 0.0), [ratingCount] 리뷰 수.
 * - [distanceMeters] 기준점까지의 직선 거리(미터, 검색엔진 geo_distance 값).
 */
data class NearbyPlaceSummary(
    val id: Long,
    val name: String,
    val category: String,
    val imageUrl: String?,
    val rating: Double,
    val ratingCount: Int,
    val distanceMeters: Double,
)
