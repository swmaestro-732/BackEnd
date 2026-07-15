package com.example.backend.place.adapter.inbound.web

/**
 * 장소 검색 정렬 기준(디자인 · 검색 결과 · 장소 탭 정렬 드롭다운).
 * - [DISTANCE] 거리순(기본): 지도 뷰포트/사용자 위치 기준 가까운 순.
 * - [REVIEW] 리뷰순: 리뷰 수 많은 순.
 */
enum class PlaceSearchSort {
    DISTANCE,
    REVIEW,
}
