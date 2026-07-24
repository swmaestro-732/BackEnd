package com.example.backend.place.application.port.outbound

/**
 * 아웃바운드 포트 — 장소 조회 계약. 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface PlaceQueryPort {
    fun findCategoryNames(placeIds: List<Long>): Map<Long, String>
}
