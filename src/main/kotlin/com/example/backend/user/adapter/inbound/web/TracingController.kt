package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.CheckInRequest
import com.example.backend.user.adapter.inbound.web.response.TracingProgressResponse
import com.example.backend.user.adapter.inbound.web.response.TracingStartResponse
import com.example.backend.user.application.port.inbound.TraceCourseUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 따라가기(SCRUM-433 · 따라가기 시작 · 장소 방문 체크인 · 진행 조회).
 *
 * - [start] 따라가기 시작(`POST /api/v1/courses/{courseId}/tracings`): **실구현** — tracing_courses 행을 만든다.
 *   이미 진행중이면 409. 저장 주체 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받는다.
 * - [checkIn] 장소 체크인(`POST /api/v1/tracings/{tracingId}/check-ins`): **실구현** — 코스 소속 장소만 체크인한다(그 외 400).
 *   서로 다른 코스 장소를 모두 채우면 자동 완주된다(courses.tracings_cnt 증가).
 * - [progress] 진행 조회(`GET /api/v1/tracings/{tracingId}`): **실구현** — 체크인 수 대비 코스 전체 장소 수·완주 여부.
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 고정 목을 반환한다(코스 저장 선례와 동일 규칙).
 */
@RestController
class TracingController(
    private val traceCourseUseCase: TraceCourseUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping("/api/v1/courses/{courseId}/tracings")
    @ResponseStatus(HttpStatus.CREATED)
    fun start(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<TracingStartResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(TracingStartResponse.mock(courseId))

        return ApiResponse.success(TracingStartResponse.from(traceCourseUseCase.start(userId, courseId)))
    }

    @PostMapping("/api/v1/tracings/{tracingId}/check-ins")
    fun checkIn(
        @CurrentUserId userId: Long,
        @PathVariable tracingId: Long,
        @Valid @RequestBody request: CheckInRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<TracingProgressResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(TracingProgressResponse.mock(tracingId))

        return ApiResponse.success(
            TracingProgressResponse.from(traceCourseUseCase.checkInPlace(userId, tracingId, request.placeId)),
        )
    }

    @GetMapping("/api/v1/tracings/{tracingId}")
    fun progress(
        @CurrentUserId userId: Long,
        @PathVariable tracingId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<TracingProgressResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(TracingProgressResponse.mock(tracingId))

        return ApiResponse.success(TracingProgressResponse.from(traceCourseUseCase.getProgress(userId, tracingId)))
    }
}
