package com.example.backend.course.application.port.inbound.dto

import com.example.backend.common.domain.CourseTraceStatus
import java.time.Instant

/**
 * 따라가기 상세(진행 중·완주) — 세션·코스 요약·따라가는 장소·체크인. 코스를 더 볼 수 없어도 장소는 내려주고 요약만 isDeleted 다.
 * [places] 는 루트를 수정해 시작한 세션이면 그 루트다 — 코스 밖 장소는 id 0 에 캡션·사진이 없다.
 */
data class CourseTraceDetailResult(
    val traceId: Long,
    val status: CourseTraceStatus,
    val startedAt: Instant,
    /** COMPLETED 일 때만. */
    val endedAt: Instant?,
    val durationMinutes: Int,
    val distanceMeters: Int,
    val course: TracedCourseSummary,
    val places: List<CoursePlaceResult>,
    val visitedPlaces: List<VisitedPlaceResult>,
)
