package com.example.backend.common.geo

/**
 * 도메인 중립 좌표 값 타입 — 위도(latitude)/경도(longitude), WGS84.
 *
 * common 에 두어 어떤 도메인(place/course/…)이나 direction 슬라이스에서 공유한다.
 */
data class Coordinate(
    val latitude: Double,
    val longitude: Double,
)
