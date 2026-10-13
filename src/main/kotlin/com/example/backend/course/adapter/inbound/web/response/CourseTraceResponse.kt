package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseTraceResult
import java.time.Instant

/** 따라가기 시작(또는 이어가기) 응답 — 세션 id·시작 시각·지금까지 체크인한 장소. 이어가기면 visitedPlaces 가 차 있다. */
data class CourseTraceResponse(
    val traceId: Long,
    val courseId: Long,
    val startedAt: Instant,
    val visitedPlaces: List<VisitedPlaceResponse>,
) {
    data class VisitedPlaceResponse(
        val placeId: Long,
        val visitedAt: Instant,
    )

    companion object {
        fun from(result: CourseTraceResult) =
            CourseTraceResponse(
                traceId = result.traceId,
                courseId = result.courseId,
                startedAt = result.startedAt,
                visitedPlaces = result.visitedPlaces.map { VisitedPlaceResponse(it.placeId, it.visitedAt) },
            )
    }
}
