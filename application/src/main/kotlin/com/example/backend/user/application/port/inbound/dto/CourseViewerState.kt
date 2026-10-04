package com.example.backend.user.application.port.inbound.dto

import java.time.Instant

/**
 * 특정 사용자의 코스 관점 상태 — 코스 저장 여부와 따라가기(트레이싱=완주) 시각.
 * 코스 상세 화면의 조회자 상태(viewer)·저장함 코스 탭의 완주 표기에 쓴다.
 *
 * - [completedAt] 따라간(완주) 시각. 따라간 적 없으면 null.
 * - [hasStartedCourse] 따라간 적이 있는지 — completedAt 존재 여부로 파생한다(같은 tracing_courses 행 기준).
 */
data class CourseViewerState(
    val courseId: Long,
    val hasSaved: Boolean,
    val completedAt: Instant?,
) {
    val hasStartedCourse: Boolean get() = completedAt != null
}
