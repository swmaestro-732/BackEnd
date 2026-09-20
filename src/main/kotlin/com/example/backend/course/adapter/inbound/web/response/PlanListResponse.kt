package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.PlanSummary
import com.example.backend.course.application.port.inbound.dto.PlansResult
import java.time.Instant
import java.time.LocalDate

/** 내 계획 목록(노션 명세 · Course · 내 계획 목록 조회) — 최근 수정순 + 커서 페이지 메타. */
data class PlanListResponse(
    val nextCursor: String?,
    val hasNext: Boolean,
    val plans: List<PlanSummaryResponse>,
) {
    companion object {
        fun from(result: PlansResult) =
            PlanListResponse(
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                plans = result.plans.map(PlanSummaryResponse::from),
            )

        /** `?mock=true` 폴백 고정 목록. */
        val MOCK =
            PlanListResponse(
                nextCursor = null,
                hasNext = false,
                plans =
                    listOf(
                        PlanSummaryResponse(
                            planId = 1L,
                            title = "토요일 성수 데이트",
                            plannedDate = LocalDate.parse("2026-09-20"),
                            placeCount = 2,
                            updatedAt = Instant.parse("2026-09-18T08:00:00Z"),
                        ),
                        PlanSummaryResponse(
                            planId = 2L,
                            title = "",
                            plannedDate = null,
                            placeCount = 1,
                            updatedAt = Instant.parse("2026-09-17T12:30:00Z"),
                        ),
                    ),
            )
    }
}

data class PlanSummaryResponse(
    val planId: Long,
    val title: String,
    val plannedDate: LocalDate?,
    val placeCount: Int,
    val updatedAt: Instant,
) {
    companion object {
        fun from(summary: PlanSummary) =
            PlanSummaryResponse(
                planId = summary.id,
                title = summary.title,
                plannedDate = summary.plannedDate,
                placeCount = summary.placeCount,
                updatedAt = summary.updatedAt,
            )
    }
}
