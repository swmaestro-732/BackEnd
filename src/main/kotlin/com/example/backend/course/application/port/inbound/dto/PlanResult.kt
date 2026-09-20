package com.example.backend.course.application.port.inbound.dto

import java.time.Instant
import java.time.LocalDate

/** 계획 상세 결과 — 계획 레코드(장소는 placeId·순서·메모)만 담는다. 장소 이름·좌표 조합은 BFF 몫. */
data class PlanDetailResult(
    val id: Long,
    val title: String,
    val memo: String?,
    val plannedDate: LocalDate?,
    val sourceCourseId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** orderNo 오름차순. */
    val places: List<PlanPlaceResult>,
)

data class PlanPlaceResult(
    val placeId: Long,
    val orderNo: Int,
    val memo: String?,
)

/**
 * 내 계획 목록 조회 명령.
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null). (updatedAt, id) 기반 불투명 커서.
 * - size: 페이지 크기(1~50). 웹 어댑터에서 검증한다.
 */
data class PlansQuery(
    val userId: Long,
    val cursor: String?,
    val size: Int,
)

/** 내 계획 목록 결과 — 최근 수정순(updatedAt desc, id desc) + 커서 페이지 메타. */
data class PlansResult(
    val nextCursor: String?,
    val hasNext: Boolean,
    val plans: List<PlanSummary>,
)

data class PlanSummary(
    val id: Long,
    val title: String,
    val plannedDate: LocalDate?,
    val placeCount: Int,
    val updatedAt: Instant,
)
