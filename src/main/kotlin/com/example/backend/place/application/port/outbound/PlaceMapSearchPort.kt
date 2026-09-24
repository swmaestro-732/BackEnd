package com.example.backend.place.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.dto.PlaceMapSort

/**
 * 지도 검색 — 목록용 from/size 제한 없이 뷰포트 안 전체를 격자 집계한다. 미가용·실패는 `PLACE_SEARCH_UNAVAILABLE`(503).
 * [sort] 는 히트(ids)와 단일 셀 버킷 순서에 적용되며 DISTANCE 는 [PlaceSearchCriteria.anchor] 를 기준점으로 한다(필수).
 * [PlaceMapHits.buckets] 는 단일 셀(정렬 순) 뒤에 클러스터(키 순)가 온다.
 */
interface PlaceMapSearchPort {
    fun searchMap(
        criteria: PlaceSearchCriteria,
        precision: Int,
        sort: PlaceMapSort,
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
