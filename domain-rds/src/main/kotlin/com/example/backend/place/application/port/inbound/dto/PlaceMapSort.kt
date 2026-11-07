package com.example.backend.place.application.port.inbound.dto

/** 지도 검색 정렬 — RELEVANCE=_score, DISTANCE=기준점(사용자 위치, 없으면 뷰포트 중심)에서 가까운 순. */
enum class PlaceMapSort {
    RELEVANCE,
    DISTANCE,
}
