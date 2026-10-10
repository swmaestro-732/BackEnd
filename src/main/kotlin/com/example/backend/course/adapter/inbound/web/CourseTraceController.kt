package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.EndCourseTraceRequest
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceCommand
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 따라가기. 노션 API 명세서 Course › course-track "따라가기 종료" 기준.
 * 요청 필드(방문 장소·소요 시간·이동 거리)는 디자인(따라가기 4 · 완료 화면)에서 도출했다.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/trace")
class CourseTraceController(
    private val courseTraceUseCase: CourseTraceUseCase,
) {
    /** 따라가기 종료 — 기록·방문 장소 삽입 + tracings_cnt +1. `/api` 하위는 permitAll 이라 access 토큰을 직접 강제한다. */
    @PostMapping
    @AccessTokenRequired
    fun end(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: EndCourseTraceRequest,
    ): ApiResponse<Nothing?> {
        courseTraceUseCase.end(
            EndCourseTraceCommand(
                userId = userId,
                courseId = courseId,
                visitedPlaces =
                    request.visitedPlaces.map {
                        VisitedPlaceCommand(
                            placeId = it.placeId,
                            visitedAt = it.visitedAt,
                        )
                    },
                durationMinutes = request.durationMinutes,
                distanceMeters = request.distanceMeters,
            ),
        )
        return ApiResponse.ok("따라가기를 종료했습니다.")
    }
}
