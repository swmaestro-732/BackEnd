package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreatePlanRequest
import com.example.backend.course.adapter.inbound.web.request.EditPlanRequest
import com.example.backend.course.adapter.inbound.web.response.PlanDetailResponse
import com.example.backend.course.adapter.inbound.web.response.PlanIdResponse
import com.example.backend.course.adapter.inbound.web.response.PlanListResponse
import com.example.backend.course.application.port.inbound.PlanQueryUseCase
import com.example.backend.course.application.port.inbound.PlanUseCase
import com.example.backend.course.application.port.inbound.dto.PlansQuery
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 인바운드 어댑터 — 코스 계획 */
@RequiresAppFeature(AppFeature.PLAN)
@AccessTokenRequired
@RestController
@RequestMapping("/api/v1/plans")
class PlanController(
    private val planUseCase: PlanUseCase,
    private val planQueryUseCase: PlanQueryUseCase,
    private val mockGuard: MockGuard,
) {
    /** 계획 생성 */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreatePlanRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlanIdResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(PlanIdResponse.MOCK)
        return ApiResponse.success(PlanIdResponse.from(planUseCase.create(request.toCommand(userId))))
    }

    /** 내 계획 목록 */
    @GetMapping("")
    fun list(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlanListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(PlanListResponse.MOCK)
        val result = planQueryUseCase.list(PlansQuery(userId = userId, cursor = cursor, size = size))
        return ApiResponse.success(PlanListResponse.from(result))
    }

    /** 계획 상세 — 소유자만. */
    @GetMapping("/{planId}")
    fun getDetail(
        @CurrentUserId userId: Long,
        @PathVariable planId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlanDetailResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(PlanDetailResponse.MOCK)
        return ApiResponse.success(PlanDetailResponse.from(planQueryUseCase.getDetail(planId, userId)))
    }

    /** 계획 편집(전체 치환). */
    @PatchMapping("/{planId}")
    fun edit(
        @CurrentUserId userId: Long,
        @PathVariable planId: Long,
        @Valid @RequestBody request: EditPlanRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlanIdResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(PlanIdResponse.MOCK)
        return ApiResponse.success(PlanIdResponse.from(planUseCase.edit(request.toCommand(userId, planId))))
    }

    /** 계획 삭제(소프트 삭제). */
    @DeleteMapping("/{planId}")
    fun delete(
        @CurrentUserId userId: Long,
        @PathVariable planId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("계획이 삭제되었습니다.")
        planUseCase.delete(userId = userId, planId = planId)
        return ApiResponse.ok("계획이 삭제되었습니다.")
    }
}
