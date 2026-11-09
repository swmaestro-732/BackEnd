package com.example.backend.place.application.port.inbound.dto

/** 지도 전체 응답. places는 개별 마커, clusters는 여러 장소를 묶은 마커다. 커서는 없다. */
data class PlaceMapResult(
    val totalCount: Long,
    val places: List<PlaceMapMarker>,
    val clusters: List<PlaceMapCluster>,
)

/** 개별 마커 — 검색 색인에 실린 필드만 담는다. */
data class PlaceMapMarker(
    val id: Long,
    val name: String,
    val category: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val categoryDetail: String? = null,
)

data class PlaceMapCluster(
    val key: String,
    val latitude: Double,
    val longitude: Double,
    val count: Long,
)
