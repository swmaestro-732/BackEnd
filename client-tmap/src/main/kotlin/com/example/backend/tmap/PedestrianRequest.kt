package com.example.backend.tmap

/** T map 보행자 경로 API 원시 요청. X=경도, Y=위도. */
data class PedestrianRequest(
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
    val startName: String = "출발",
    val endName: String = "도착",
)
