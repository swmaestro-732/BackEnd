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
