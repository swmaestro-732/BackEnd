package com.example.backend.place.application.port.inbound

import com.example.backend.place.domain.model.Place

/**
 * 인바운드 포트 — 장소 조회(공개 API). 다른 도메인(course)이 장소 정보를 필요로 할 때 이 포트로만 접근한다.
 */
interface PlaceQueryUseCase {
    /** placeIds 에 해당하는 장소들. 존재하지 않거나 삭제된 장소는 결과에서 빠진다. */
    fun findPlacesById(placeIds: List<Long>): List<Place>
}
