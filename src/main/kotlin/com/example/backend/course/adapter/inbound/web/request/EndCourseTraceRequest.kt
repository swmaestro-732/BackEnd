package com.example.backend.course.adapter.inbound.web.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import java.time.Instant

/** 따라가기 종료 요청 — 방문한 장소 기록과 소요 시간·이동 거리(클라이언트가 측정해 보낸다). */
data class EndCourseTraceRequest(
    @field:Valid
    val visitedPlaces: List<VisitedPlace> = emptyList(),
    @field:Min(0)
    val durationMinutes: Int,
    @field:Min(0)
    val distanceMeters: Int,
) {
    /** 방문한 장소 한 곳 — 도착(방문 체크) 시각은 UTC. */
    data class VisitedPlace(
        @field:Positive
        val placeId: Long,
        val visitedAt: Instant,
    )
}
