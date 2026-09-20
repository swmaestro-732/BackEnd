package com.example.backend.course.application.port.outbound

import com.example.backend.course.domain.model.Plan
import java.time.Instant
import java.time.LocalDate

/** 내 계획 목록 키셋 커서 — 정렬 키(updated_at DESC, id DESC)가 가리키는 마지막 행. */
data class PlanCursor(
    val updatedAt: Instant,
    val id: Long,
)

/** 계획 요약 읽기 모델(목록용). placeCount 는 plan_places 행 수. */
data class PlanSummaryRow(
    val id: Long,
    val title: String,
    val plannedDate: LocalDate?,
    val placeCount: Int,
    val updatedAt: Instant,
)

/** 아웃바운드 포트 — 계획 애그리거트(plans·plan_places) 영속. 모든 조회는 deleted_at IS NULL 인 행만 본다. */
interface PlanPersistencePort {
    /** 계획 본문과 장소(orderNo 오름차순)를 읽어 애그리거트로 복원한다. 없거나 삭제됐으면 null. */
    fun findById(planId: Long): Plan?

    /** 소유자의 계획 요약을 updatedAt DESC, id DESC 로 [cursor] 이후부터 최대 [size] 건 읽는다(hasNext 판별용 +1 은 호출부가 더한다). */
    fun findSummariesByOwner(
        userId: Long,
        cursor: PlanCursor?,
        size: Int,
    ): List<PlanSummaryRow>

    /** 계획·장소를 저장하고 생성값(id·타임스탬프)까지 채운 계획을 반환한다. */
    fun save(plan: Plan): Plan

    /** 영속화된 계획([Plan.id] 필수)을 전체 치환한다 — 본문 갱신 후 장소를 지우고 다시 심는다. */
    fun update(plan: Plan): Plan

    /** deleted_at·updated_at 스탬프만 찍는다. 이미 삭제된 행은 건드리지 않으며 영향받은 행 수를 반환한다. */
    fun softDelete(planId: Long): Int
}
