package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.PlanDetailResult
import com.example.backend.course.application.port.inbound.dto.PlansQuery
import com.example.backend.course.application.port.inbound.dto.PlansResult

/** 인바운드 포트 — 계획 조회. 소유자 본인 것만 반환하며 없음·삭제·타인 소유는 404(PLAN_NOT_FOUND). */
interface PlanQueryUseCase {
    fun getDetail(
        planId: Long,
        userId: Long,
    ): PlanDetailResult

    /** 내 계획 목록 — 최근 수정순, 커서 페이지. */
    fun list(query: PlansQuery): PlansResult
}
