package com.example.backend.place.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** 지도 검색 — 목록용 from/size 제한 없이 뷰포트 안 전체를 격자 집계한다. 미가용·실패는 `PLACE_SEARCH_UNAVAILABLE`(503). */
interface PlaceMapSearchPort {
    fun searchMap(
        criteria: PlaceSearchCriteria,
        precision: Int,
    ): PlaceMapHits
}

data class PlaceMapHits(
    val totalCount: Long,
    val ids: List<Long>,
    val buckets: List<PlaceMapBucket>,
)

data class PlaceMapBucket(
    val key: String,
    val center: Coordinate,
    val count: Long,
    val singlePlaceId: Long?,
)
