package com.example.backend.place.application.port.inbound

/**
 * 인바운드 포트 — 장소 조회(공개 API). 다른 도메인(course)이 장소 정보를 필요로 할 때 이 포트로만 접근한다.
 */
interface PlaceQueryUseCase {
    /** placeId → 카테고리 이름(PlaceCategory.name). 존재하지 않거나 삭제된 장소는 결과에서 빠진다. */
    fun findCategoryNames(placeIds: List<Long>): Map<Long, String>
}
