package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/**
 * 내 완주 목록 조회 명령.
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null). id 기반 불투명 커서.
 * - size: 페이지 크기(1~50). 웹 어댑터에서 검증한다.
 */
data class CourseTracesQuery(
    val userId: Long,
    val cursor: String?,
    val size: Int,
)

/** 내 완주 목록 결과 — 최근 시작순(id desc) + 커서 페이지 메타. */
data class CourseTracesResult(
    val nextCursor: String?,
    val hasNext: Boolean,
    val traces: List<CompletedCourseTrace>,
)

/** 완주 목록 한 줄 — 세션 기록 + 코스 요약. */
data class CompletedCourseTrace(
    val trace: CourseTraceSummary,
    val course: TracedCourseSummary,
)

/** 완주한 코스 요약. 더 볼 수 없는 코스(삭제·비활성·비공개 전환)는 courseId 외 값이 모두 null 이고 isDeleted 가 true 다. */
data class TracedCourseSummary(
    val courseId: Long,
    val title: String?,
    val coverImageUrl: String?,
    val area: String?,
    val placeCount: Int?,
    val isDeleted: Boolean,
) {
    companion object {
        fun deleted(courseId: Long) =
            TracedCourseSummary(
                courseId = courseId,
                title = null,
                coverImageUrl = null,
                area = null,
                placeCount = null,
                isDeleted = true,
            )
    }
}

/** 따라가기 한 건 — 세션 기록만 담는다. 체크인 목록은 시작(이어가기) 응답에. */
data class CourseTraceSummary(
    val id: Long,
    val courseId: Long,
    val startedAt: Instant,
    /** COMPLETED 일 때만. */
    val endedAt: Instant?,
    val durationMinutes: Int,
    val distanceMeters: Int,
)
