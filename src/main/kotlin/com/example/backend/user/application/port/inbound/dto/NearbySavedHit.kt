package com.example.backend.user.application.port.inbound.dto

/** 거리순 저장 장소 검색 결과. 상세·평점·방문 여부는 DB 조회로 채운다(OS 는 랭킹만). */
data class NearbySavedHit(
    val placeId: Long,
    val distanceMeters: Double,
)
