package com.example.backend.place.application.port.inbound

import com.example.backend.place.application.port.inbound.dto.PlaceSummary

/**
 * 인바운드 포트 — 장소 조회(공개 API). 다른 도메인(course)이 장소 정보를 필요로 할 때 이 포트로만 접근한다.
 * 도메인 모델 대신 [PlaceSummary] 를 반환해 장소 내부 모델이 경계 밖으로 새지 않게 한다.
 */
interface PlaceQueryUseCase {
    /** placeIds 에 해당하는 장소 요약들. 존재하지 않거나 삭제된 장소는 결과에서 빠진다. */
    fun findPlacesById(placeIds: List<Long>): List<PlaceSummary>

    /** 이름에 [query] 를 포함하는 장소 요약들. 공백/빈 검색어는 빈 결과. 삭제된 장소는 빠진다. */
    fun searchByName(query: String): List<PlaceSummary>
}
