package com.example.backend.direction.application.port.inbound.dto

import com.example.backend.common.geo.Coordinate

/** 두 지점 사이 도보 경로 — 거리(m)·소요 초·지도에 그릴 폴리라인·안내 지점. */
data class WalkingRouteResult(
    val distanceMeters: Int,
    val seconds: Int,
    val path: List<Coordinate>,
    val guides: List<WalkingGuideResult>,
)

data class WalkingGuideResult(
    val coordinate: Coordinate,
    val description: String,
)
