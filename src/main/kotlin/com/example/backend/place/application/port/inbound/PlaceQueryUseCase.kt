package com.example.backend.place.application.port.inbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.inbound.dto.PlaceSummaryPage

/**
 * 인바운드 포트 — 장소 조회(공개 API). 다른 도메인(course)이 장소 정보를 필요로 할 때 이 포트로만 접근한다.
 * 도메인 모델 대신 [PlaceSummary] 를 반환해 장소 내부 모델이 경계 밖으로 새지 않게 한다.
 */
interface PlaceQueryUseCase {
    /** placeIds 에 해당하는 장소 요약들. 존재하지 않거나 삭제된 장소는 결과에서 빠진다. */
    fun findPlacesById(placeIds: List<Long>): List<PlaceSummary>

    /**
     * 코스 생성 목록 검색. 기준 장소가 있으면 거리 가산점을 적용한다. 매 페이지 같은 검색어·기준 장소를 보낸다.
     * [areaCodePrefixes] 가 있으면 그 지역(법정동코드 prefix 중 하나)으로 먼저 거르고, 0건이면 지역 없이 다시 찾는다.
     */
    fun searchByName(
        query: String,
        cursor: String?,
        size: Int,
        anchorPlaceId: Long? = null,
        areaCodePrefixes: List<String> = emptyList(),
    ): PlaceSummaryPage

    /**
     * 좌표가 속한 법정동코드 추정 — 반경 1.5km 안 가까운 장소 20곳이 가진 코드 중 가장 많은 값(행정구역 경계 데이터 없이).
     * 근처에 장소가 없으면 null, 검색 엔진을 못 쓰면 `PLACE_SEARCH_UNAVAILABLE`(503).
     */
    fun findDominantAreaCodeNear(location: Coordinate): String?
}
