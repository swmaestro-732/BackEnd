package com.example.backend.course.adapter.inbound.web.v2.response

import com.example.backend.course.adapter.inbound.web.response.CourseTraceResponse.VisitedPlaceResponse
import com.example.backend.course.application.port.inbound.dto.CourseTraceResult
import java.time.Instant

/** 따라가기 시작 V2(또는 이어가기) 응답 — V1 응답에 세션이 따라가는 장소(`placeIds`, 순서대로)를 더한다. 이어가기면 기존 세션의 루트다. */
data class CourseTraceV2Response(
    val traceId: Long,
    val courseId: Long,
    val startedAt: Instant,
    val placeIds: List<Long>,
    val visitedPlaces: List<VisitedPlaceResponse>,
) {
    companion object {
        fun from(result: CourseTraceResult) =
            CourseTraceV2Response(
                traceId = result.traceId,
                courseId = result.courseId,
                startedAt = result.startedAt,
                placeIds = result.placeIds,
                visitedPlaces = result.visitedPlaces.map { VisitedPlaceResponse(it.placeId, it.visitedAt) },
            )
    }
}
