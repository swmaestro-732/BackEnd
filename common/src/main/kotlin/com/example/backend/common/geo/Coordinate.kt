package com.example.backend.common.geo

/**
 * 도메인 중립 좌표 값 타입 — 위도(latitude)/경도(longitude), WGS84.
 *
 * common 에 두어 어떤 도메인(place/course/…)이나 direction 슬라이스에서 공유한다.
 */
data class Coordinate(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "위도는 -90~90 범위여야 합니다: $latitude" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "경도는 -180~180 범위여야 합니다: $longitude" }
    }
}
