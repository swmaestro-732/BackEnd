package com.example.backend.common.geo

/**
 * 지도 뷰포트 — 남서(sw)·북동(ne) 모서리로 정의한 사각 영역. sw ≤ ne 여야 하며 자오선(180°) 교차는 지원하지 않는다.
 */
data class Viewport(
    val southWest: Coordinate,
    val northEast: Coordinate,
) {
    init {
        require(southWest.latitude <= northEast.latitude) { "뷰포트 남서 위도가 북동 위도보다 클 수 없습니다." }
        require(southWest.longitude <= northEast.longitude) { "뷰포트 남서 경도가 북동 경도보다 클 수 없습니다." }
    }
}
