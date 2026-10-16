package com.example.backend.place.application.port.inbound.dto

/** 지도 전체 응답. places는 개별 마커, clusters는 여러 장소를 묶은 마커다. 커서는 없다. */
data class PlaceMapResult(
    val totalCount: Long,
    val places: List<PlaceSummary>,
    val clusters: List<PlaceMapCluster>,
)

data class PlaceMapCluster(
    val key: String,
    val latitude: Double,
    val longitude: Double,
    val count: Long,
)
