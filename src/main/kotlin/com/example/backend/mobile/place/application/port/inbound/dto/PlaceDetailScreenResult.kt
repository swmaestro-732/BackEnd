package com.example.backend.mobile.place.application.port.inbound.dto

/**
 * 장소 상세 화면 조합 결과 (BFF). 지금은 장소(place) 정보만 담는다.
 * 리뷰·이 근처 코스·저장 여부는 백엔드가 생기면 이 결과에 확장한다(MVP 범위: 웹 응답에서 빈/false 스텁).
 */
data class PlaceDetailScreenResult(
    val id: Long,
    val name: String,
    val category: String,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val address: String,
)
