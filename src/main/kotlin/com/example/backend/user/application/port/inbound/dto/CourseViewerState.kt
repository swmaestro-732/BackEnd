package com.example.backend.user.application.port.inbound.dto

import java.time.Instant

/**
 * 특정 사용자의 코스 관점 상태 — 코스 저장 여부와 따라가기(트레이싱=완주) 시각.
 * 코스 상세 화면의 조회자 상태(viewer)·저장함 코스 탭의 완주 표기에 쓴다.
 *
 * - [completedAt] 따라간(완주) 시각. 완주한 적 없으면 null(완주 = completed_at IS NOT NULL).
 * - [hasStarted] 따라가기를 시작한 적이 있는지(tracing_courses 행 존재 — 진행중 포함).
 * - [hasStartedCourse] 시작 여부 — [hasStarted] 로 파생한다(속성명은 다운스트림 호환 위해 유지).
 */
data class CourseViewerState(
    val courseId: Long,
    val hasSaved: Boolean,
    val hasStarted: Boolean,
    val completedAt: Instant?,
) {
    val hasStartedCourse: Boolean get() = hasStarted
}
