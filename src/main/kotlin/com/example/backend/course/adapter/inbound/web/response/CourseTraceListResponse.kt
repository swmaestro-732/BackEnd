package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseTraceSummary
import com.example.backend.course.application.port.inbound.dto.CourseTracesResult
import com.example.backend.course.domain.model.CourseTraceStatus
import java.time.Instant

/** 내 따라가기 목록 응답 — 최근 시작순 + 커서 페이지 메타. status=IN_PROGRESS 로 조회해 traces 가 비어 있으면 따라가는 중인 코스가 없다. */
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

/** 따라가기 한 건 — endedAt 은 완주일 때만, 소요 시간·거리는 종료 전엔 0. 체크인 목록은 시작(이어가기) 응답으로 받는다. */
data class CourseTraceSummaryResponse(
    val traceId: Long,
    val courseId: Long,
    val status: CourseTraceStatus,
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
                status = summary.status,
                startedAt = summary.startedAt,
                endedAt = summary.endedAt,
                durationMinutes = summary.durationMinutes,
                distanceMeters = summary.distanceMeters,
            )
    }
}
