package com.example.backend.course.application.port.inbound.dto

import com.example.backend.common.geo.Coordinate

/** 따라가기 경로 — 출발·도착 장소와 도보 경로. [route] 가 null 이면 도보로 갈 수 없는 구간이다. */
data class TraceRouteResult(
    val from: TracePlaceResult,
    val to: TracePlaceResult,
    val route: TraceWalkingRouteResult?,
)

data class TracePlaceResult(
    val placeId: Long,
    val name: String,
    val coordinate: Coordinate,
)

data class TraceWalkingRouteResult(
    val distanceMeters: Int,
    val seconds: Int,
    val path: List<Coordinate>,
    val guides: List<TraceRouteGuideResult>,
)

data class TraceRouteGuideResult(
    val coordinate: Coordinate,
    val description: String,
)
