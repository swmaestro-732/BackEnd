package com.example.backend.place.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** 엔진 미가용 시 null. 지도 검색에는 목록용 from/size 제한을 적용하지 않는다. */
interface PlaceMapSearchPort {
    fun searchMap(
        criteria: PlaceSearchCriteria,
        precision: Int,
    ): PlaceMapHits?
}

interface PlaceMapQueryPort {
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
