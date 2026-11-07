package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CompletedCourseTrace
import com.example.backend.course.application.port.inbound.dto.CourseTracesResult
import com.example.backend.course.application.port.inbound.dto.TracedCourseSummary
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
    val startedAt: Instant,
    val endedAt: Instant?,
    val durationMinutes: Int,
    val distanceMeters: Int,
    val course: TracedCourseResponse,
) {
    companion object {
        fun from(completed: CompletedCourseTrace) =
            CourseTraceSummaryResponse(
                traceId = completed.trace.id,
                startedAt = completed.trace.startedAt,
                endedAt = completed.trace.endedAt,
                durationMinutes = completed.trace.durationMinutes,
                distanceMeters = completed.trace.distanceMeters,
                course = TracedCourseResponse.from(completed.course),
            )
    }
}

/** 완주한 코스 요약 — isDeleted 가 true 면 courseId 외 값은 모두 null 이다. */
data class TracedCourseResponse(
    val courseId: Long,
    val title: String?,
    val coverImageUrl: String?,
    val area: String?,
    val placeCount: Int?,
    val isDeleted: Boolean,
) {
    companion object {
        fun from(course: TracedCourseSummary) =
            TracedCourseResponse(
                courseId = course.courseId,
                title = course.title,
                coverImageUrl = course.coverImageUrl,
                area = course.area,
                placeCount = course.placeCount,
                isDeleted = course.isDeleted,
            )
    }
}
