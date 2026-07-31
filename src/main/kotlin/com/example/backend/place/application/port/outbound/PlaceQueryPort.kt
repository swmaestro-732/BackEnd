package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.Place

/**
 * 아웃바운드 포트 — 장소 조회 계약. 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface PlaceQueryPort {
    /** placeIds 에 해당하는 장소들. 존재하지 않거나 삭제된 장소는 결과에서 빠진다. */
    fun findPlacesById(placeIds: List<Long>): List<Place>

    /** 이름에 [query] 를 포함하는(부분 일치) 장소들. 삭제된 장소는 결과에서 빠진다. */
    fun searchByName(query: String): List<Place>
}
