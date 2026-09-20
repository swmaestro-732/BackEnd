package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.PlanDetailScreenResponse
import com.example.backend.mobile.course.application.port.inbound.PlanScreenUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 계획 상세 **화면 조합 API** (BFF) — `GET /service/v1/my/plans/{planId}`.
 *
 * 도메인 API(`GET /api/v1/plans/{planId}`)는 장소를 placeId·순서·메모로만 주므로, 지도 핀·장소 카드를 그리려면
 * 이 API 를 쓴다. 경로가 `/service/v1/my` 하위라 SecurityConfig 경로 매처가 JWT 를 강제한다(소유자 식별은 JWT subject).
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 조회 없이 고정 목([PlanDetailScreenResponse.MOCK])을 반환한다.
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RequiresAppFeature(AppFeature.PLAN)
@RestController
@RequestMapping("/service/v1")
class PlanScreenController(
    private val planScreenUseCase: PlanScreenUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping("/my/plans/{planId}")
    fun getScreen(
        @PathVariable planId: Long,
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlanDetailScreenResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(PlanDetailScreenResponse.MOCK)

        return ApiResponse.success(PlanDetailScreenResponse.from(planScreenUseCase.getScreen(planId, userId)))
    }
}
