package com.example.backend.direction.adapter.outbound.tmap

/** T map 보행자 경로 API 원시 응답 — 어댑터 밖으로 노출하지 않는다(모듈 internal). */
internal data class PedestrianResponse(
    val features: List<Feature>? = null,
) {
    data class Feature(
        val properties: Properties? = null,
    )

    data class Properties(
        val totalTime: Int? = null,
    )
}

/** T map 에러 응답(4xx) — `code` 로 NoServiceArea(서비스 불가 구간) 등을 구분한다. */
internal data class TmapErrorResponse(
    val error: TmapError? = null,
) {
    data class TmapError(
        val code: String? = null,
    )
}
