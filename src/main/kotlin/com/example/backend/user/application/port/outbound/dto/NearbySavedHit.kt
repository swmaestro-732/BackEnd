package com.example.backend.user.application.port.outbound.dto

/** 거리순 저장 장소 검색 결과. 상세와 평점은 place 의 DB 조회로 채운다. */
data class NearbySavedHit(
    val placeId: Long,
    val distanceMeters: Double,
    val visited: Boolean,
)
