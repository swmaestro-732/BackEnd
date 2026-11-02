package com.example.backend.course.application.port.inbound.dto

import com.example.backend.course.domain.model.CourseTraceStatus
import java.time.Instant

/**
 * 내 따라가기 목록 조회 명령.
 * - status: 진행 중(IN_PROGRESS) 또는 완주(COMPLETED) — 한 상태만 본다.
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null). id 기반 불투명 커서.
 * - size: 페이지 크기(1~50). 웹 어댑터에서 검증한다.
 */
data class CourseTracesQuery(
    val userId: Long,
    val status: CourseTraceStatus,
    val cursor: String?,
    val size: Int,
)

/** 내 따라가기 목록 결과 — 최근 시작순(id desc) + 커서 페이지 메타. */
data class CourseTracesResult(
    val nextCursor: String?,
    val hasNext: Boolean,
    val traces: List<CourseTraceSummary>,
)

/** 따라가기 목록 한 줄 — 세션 기록만 담는다. 체크인 목록은 시작(이어가기) 응답에, 코스 정보 조합은 BFF 몫. */
data class CourseTraceSummary(
    val id: Long,
    val courseId: Long,
    val status: CourseTraceStatus,
    val startedAt: Instant,
    /** COMPLETED 일 때만. */
    val endedAt: Instant?,
    val durationMinutes: Int,
    val distanceMeters: Int,
)
