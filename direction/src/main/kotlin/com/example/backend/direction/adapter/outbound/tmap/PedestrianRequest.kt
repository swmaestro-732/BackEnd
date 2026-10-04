package com.example.backend.direction.adapter.outbound.tmap

/** T map 보행자 경로 API 원시 요청 — 어댑터 밖으로 노출하지 않는다(모듈 internal). */
internal data class PedestrianRequest(
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
    val startName: String = "출발",
    val endName: String = "도착",
)
