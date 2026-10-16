package com.example.backend.mobile.home.application.port.outbound.dto

/**
 * BFF 아웃바운드 출력 — 근처 저장 장소 한 건. place 도메인 거리순 요약에 user 도메인 방문 여부를 합친 격리 DTO다.
 * category 는 도메인 enum 이 아니라 이름 문자열, distanceMeters 는 기준점까지의 직선 거리(미터)다.
 */
data class HomeNearbyPlace(
    val placeId: Long,
    val name: String,
    val imageUrl: String?,
    val category: String,
    val rating: Double,
    val ratingCount: Int,
    val distanceMeters: Double,
    val visited: Boolean,
)
