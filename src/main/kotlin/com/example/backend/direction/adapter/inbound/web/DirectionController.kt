package com.example.backend.direction.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.direction.adapter.inbound.web.request.WalkingRequest
import com.example.backend.direction.adapter.inbound.web.response.WalkingResponse
import com.example.backend.direction.application.port.inbound.WalkingDurationUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 경로/도보 시간(`POST /api/v1/directions/walking`).
 *
 * 방문 순서대로 나열한 좌표 목록을 받아 구간별 도보 시간과 총합(분)을 내려준다.
 */
@RestController
@RequestMapping("/api/v1/directions")
class DirectionController(
    private val walkingDurationUseCase: WalkingDurationUseCase,
) {
    @PostMapping("/walking")
    fun walking(
        @RequestBody @Valid request: WalkingRequest,
    ): ApiResponse<WalkingResponse> {
        val segments = walkingDurationUseCase.walkingSegments(request.toCoordinates())
        return ApiResponse.success(WalkingResponse.from(segments))
    }
}
