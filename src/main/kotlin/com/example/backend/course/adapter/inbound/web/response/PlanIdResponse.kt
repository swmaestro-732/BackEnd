package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.domain.model.Plan

/** 계획 식별자만 담는 응답 — 생성·편집이 공유한다. 이후 화면은 계획 상세 재조회로 구성한다. */
data class PlanIdResponse(
    val planId: Long,
) {
    companion object {
        fun from(plan: Plan) = PlanIdResponse(planId = requireNotNull(plan.id))

        /** `?mock=true` 폴백 — 계획 상세 목([PlanDetailResponse.MOCK], planId=1)과 이어지도록 항상 1. */
        val MOCK = PlanIdResponse(planId = 1L)
    }
}
