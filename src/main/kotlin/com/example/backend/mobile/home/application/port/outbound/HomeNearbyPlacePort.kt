package com.example.backend.mobile.home.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace

/**
 * BFF 아웃바운드 포트 — 내 저장 장소 중 가까운 곳 조회. 저장 관계 색인(user)과 장소 상세(place)를 어댑터가 잇는다.
 * 검색엔진 장애는 검색 어댑터(OpenSearchSavedPlaceSearchAdapter)가 빈 목록으로 흡수한다(홈의 한 섹션 장애가 화면 전체를 죽이지 않게).
 */
interface HomeNearbyPlacePort {
    /** [userId] 의 저장 장소 중 [origin] 의 반경 [radiusMeters] 이내에서 가까운 순으로 최대 [limit] 개. 저장 장소가 없으면 빈 목록. */
    fun findNearbySavedPlaces(
        userId: Long,
        origin: Coordinate,
        radiusMeters: Double,
        limit: Int,
    ): List<HomeNearbyPlace>
}
