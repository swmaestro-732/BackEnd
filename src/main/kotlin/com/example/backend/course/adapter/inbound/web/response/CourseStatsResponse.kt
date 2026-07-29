package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseDetailResult

/** 코스 요약 지표. tracingCount 는 따라가기 원시 카운트(축약은 프론트에서). */
data class CourseStatsResponse(
    val placeCount: Int,
    val walkingMinutes: Int,
    val tracingCount: Int,
) {
    companion object {
        fun from(result: CourseDetailResult): CourseStatsResponse =
            CourseStatsResponse(
                placeCount = result.places.size,
                walkingMinutes = result.places.sumOf { it.walkingMinutesToNext ?: 0 },
                tracingCount = result.tracingsCnt,
            )
    }
}
