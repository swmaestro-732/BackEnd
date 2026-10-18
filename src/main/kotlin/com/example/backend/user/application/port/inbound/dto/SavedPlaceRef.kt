package com.example.backend.user.application.port.inbound.dto

/** 저장 장소 참조 — 장소 id 와 방문 여부만 담는다(홈 근처 저장 장소처럼 전체 저장 목록이 필요한 조합용). */
data class SavedPlaceRef(
    val placeId: Long,
    val visited: Boolean,
)
