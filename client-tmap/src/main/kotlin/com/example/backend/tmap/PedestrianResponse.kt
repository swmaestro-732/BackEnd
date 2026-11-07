package com.example.backend.tmap

import tools.jackson.databind.JsonNode

/**
 * T map 보행자 경로 API 원시 응답(GeoJSON FeatureCollection). 도메인 경로 변환은 이 클라이언트를 쓰는 어댑터가 한다.
 * features 는 안내 지점(Point)과 경로 선분(LineString)이 번갈아 오고, 첫 feature 의 properties 에 총 시간·거리가 있다.
 */
data class PedestrianResponse(
    val features: List<Feature>? = null,
) {
    data class Feature(
        val geometry: Geometry? = null,
        val properties: Properties? = null,
    )

    /** coordinates 는 Point 면 `[x, y]`, LineString 이면 `[[x, y], ...]` (x=경도, y=위도). */
    data class Geometry(
        val type: String? = null,
        val coordinates: JsonNode? = null,
    )

    data class Properties(
        val totalTime: Int? = null,
        val totalDistance: Int? = null,
        val description: String? = null,
    )
}

/** T map 에러 응답(4xx). `code` 로 NoServiceArea(서비스 불가 구간) 등을 구분한다. */
data class TmapErrorResponse(
    val error: TmapError? = null,
) {
    data class TmapError(
        val code: String? = null,
    )
}
