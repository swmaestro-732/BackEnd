package com.example.backend.user.application.port.inbound.dto

/**
 * 거리순 저장 장소 검색 결과. 표시용 정보(이름/카테고리/이미지/평점)는 색인에 중복 저장된 값을 그대로 돌려준다(DB 왕복 없음).
 * 방문 여부(visited)만 가변이라 조회 측에서 DB 로 따로 채운다.
 */
data class NearbySavedHit(
    val placeId: Long,
    val distanceMeters: Double,
    val name: String,
    val category: String,
    val imageUrl: String?,
    val rating: Double,
    val ratingCount: Int,
)
