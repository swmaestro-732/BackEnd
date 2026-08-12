package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.TracingResult
import java.time.Instant

/** 따라가기 시작 응답 — 생성된 tracing id·코스·시작 시각. */
data class TracingStartResponse(
    val tracingId: Long,
    val courseId: Long,
    val startedAt: Instant,
) {
    companion object {
        fun from(result: TracingResult): TracingStartResponse =
            TracingStartResponse(
                tracingId = result.tracingId,
                courseId = result.courseId,
                startedAt = result.startedAt,
            )

        /** 목 응답 — 코스 상세 목(courseId=1)과 맞춰 둔다. */
        fun mock(courseId: Long): TracingStartResponse =
            TracingStartResponse(
                tracingId = 100L,
                courseId = courseId,
                startedAt = Instant.parse("2026-07-31T00:00:00Z"),
            )
    }
}
