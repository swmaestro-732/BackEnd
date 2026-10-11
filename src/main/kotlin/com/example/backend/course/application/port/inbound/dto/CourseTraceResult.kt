package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/** 진행 중인 따라가기 세션 — 시작(또는 이어가기) 응답. 지금까지 체크인한 장소를 순서대로 담는다. */
data class CourseTraceResult(
    val traceId: Long,
    val courseId: Long,
    val startedAt: Instant,
    val visitedPlaces: List<VisitedPlaceResult>,
)

data class VisitedPlaceResult(
    val placeId: Long,
    val visitedAt: Instant,
)
