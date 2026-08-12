package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.TracingProgress
import java.time.Instant

/** 따라가기 진행 응답 — 체크인 수 대비 코스 전체 장소 수·완주 여부. */
data class TracingProgressResponse(
    val tracingId: Long,
    val courseId: Long,
    val totalPlaces: Int,
    val checkedPlaces: Int,
    val completed: Boolean,
    val completedAt: Instant?,
) {
    companion object {
        fun from(progress: TracingProgress): TracingProgressResponse =
            TracingProgressResponse(
                tracingId = progress.tracingId,
                courseId = progress.courseId,
                totalPlaces = progress.totalPlaces,
                checkedPlaces = progress.checkedPlaces,
                completed = progress.completed,
                completedAt = progress.completedAt,
            )

        /** 목 응답 — 3개 장소 중 1개 체크인한 진행중 상태. */
        fun mock(tracingId: Long): TracingProgressResponse =
            TracingProgressResponse(
                tracingId = tracingId,
                courseId = 1L,
                totalPlaces = 3,
                checkedPlaces = 1,
                completed = false,
                completedAt = null,
            )
    }
}
