package com.example.backend.mobile.course.application.port.inbound

import com.example.backend.mobile.course.application.port.inbound.dto.PlanDetailScreenResult

/** 인바운드 포트 — 계획 상세 화면 조합 (BFF). */
interface PlanScreenUseCase {
    /** 소유자 본인의 계획만 조합한다 — 없음·삭제·타인 소유 판정은 계획 도메인이 하고 404(PLAN_NOT_FOUND)로 올라온다. */
    fun getScreen(
        planId: Long,
        userId: Long,
    ): PlanDetailScreenResult
}
