package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.TraceScreenResponse
import com.example.backend.mobile.course.application.port.inbound.TraceScreenUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 따라가기 상세 **화면 조합 API** (BFF) — `GET /service/v1/my/traces/{traceId}`.
 * 홈 카드의 traceId 로 진입해 따라가기 화면을 다시 그리거나, 완주 목록의 traceId 로 기록을 본다.
 * 코스가 삭제·비공개돼도 장소는 내려주고 `course.isDeleted` 로 알린다.
 * 경로가 `/service/v1/my` 하위라 SecurityConfig 경로 매처가 JWT 를 강제한다. `?mock=true` 면 조회 없이 고정 목([TraceScreenResponse.MOCK])을 반환한다.
 */
@RequiresAppFeature(AppFeature.COURSE_TRACING)
@RestController
class TraceScreenController(
    private val traceScreenUseCase: TraceScreenUseCase,
) {
    @GetMapping("/service/v1/my/traces/{traceId}")
    fun getScreen(
        @CurrentUserId userId: Long,
        @PathVariable traceId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<TraceScreenResponse> {
        if (mock) return ApiResponse.success(TraceScreenResponse.MOCK)

        return ApiResponse.success(TraceScreenResponse.from(traceScreenUseCase.getScreen(traceId, userId)))
    }
}
