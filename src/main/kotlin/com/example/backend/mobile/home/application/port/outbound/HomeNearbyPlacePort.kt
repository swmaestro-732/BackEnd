package com.example.backend.mobile.home.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace

/**
 * BFF 아웃바운드 포트 — 내 저장 장소 중 가까운 곳 조회. 저장 목록(user)과 거리순 조회(place)를 어댑터가 잇는다.
 * 검색엔진 장애는 어댑터가 빈 목록으로 흡수한다(홈의 한 섹션 장애가 화면 전체를 죽이지 않게).
 */
interface HomeNearbyPlacePort {
    /** [userId] 의 저장 장소 중 [origin] 에서 가까운 순으로 최대 [limit] 개. 저장 장소가 없으면 빈 목록. */
    fun findNearbySavedPlaces(
        userId: Long,
        origin: Coordinate,
        limit: Int,
    ): List<HomeNearbyPlace>
}
