package com.example.backend.place.application.port.inbound.dto

/**
 * 외부 장소 검색 결과 — 내부 저장(dedup)까지 마쳐 우리 place id 를 실어 나른다.
 * 표시 필드(name·좌표·주소 등)는 검색 응답 원본(카카오)에서, id·category 는 저장된 [com.example.backend.place.domain.model.Place] 에서 온다.
 */
data class PlaceSearchResult(
    val id: Long,
    val name: String,
    val category: String,
    val roadAddress: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val telephone: String?,
)
