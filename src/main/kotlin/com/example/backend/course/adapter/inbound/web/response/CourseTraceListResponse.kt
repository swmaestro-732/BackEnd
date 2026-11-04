package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseTraceSummary
import com.example.backend.course.application.port.inbound.dto.CourseTracesResult
import java.time.Instant

/** 내 완주 목록 응답 — 최근 시작순 + 커서 페이지 메타. */
data class CourseTraceListResponse(
    val nextCursor: String?,
    val hasNext: Boolean,
    val traces: List<CourseTraceSummaryResponse>,
) {
    companion object {
        fun from(result: CourseTracesResult) =
            CourseTraceListResponse(
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                traces = result.traces.map(CourseTraceSummaryResponse::from),
            )
    }
}

/** 완주 한 건. 체크인 목록은 담지 않는다. */
data class CourseTraceSummaryResponse(
    val traceId: Long,
    val courseId: Long,
    val startedAt: Instant,
    val endedAt: Instant?,
    val durationMinutes: Int,
    val distanceMeters: Int,
) {
    companion object {
        fun from(summary: CourseTraceSummary) =
            CourseTraceSummaryResponse(
                traceId = summary.id,
                courseId = summary.courseId,
                startedAt = summary.startedAt,
                endedAt = summary.endedAt,
                durationMinutes = summary.durationMinutes,
                distanceMeters = summary.distanceMeters,
            )
    }
}
