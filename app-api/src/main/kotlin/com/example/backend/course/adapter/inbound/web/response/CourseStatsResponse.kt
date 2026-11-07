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
                // 도보 시간 합계는 양수만 더한다 — -1(도보 이동 불가)·null(마지막 장소)은 소요 시간이 아니라 제외.
                walkingMinutes =
                    result.places
                        .mapNotNull { it.walkingMinutesToNext }
                        .filter { it > 0 }
                        .sum(),
                tracingCount = result.tracingsCnt,
            )
    }
}
