package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CheckInPlaceRequest
import com.example.backend.course.adapter.inbound.web.request.EndCourseTraceRequest
import com.example.backend.course.adapter.inbound.web.response.CourseTraceResponse
import com.example.backend.course.adapter.inbound.web.response.TraceRouteResponse
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.CheckInPlaceCommand
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceCommand
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 따라가기. 노션 API 명세서 Course › course-track 기준.
 * 시작은 코스 아래(`/courses/{courseId}/traces`), 그 뒤 체크인·종료는 세션 id 로(`/traces/{traceId}/...`) 다룬다.
 * 경로 조회(`/traces?fromPlaceId&toPlaceId`)는 세션에 묶지 않는다 — 따라가는 중 코스 밖 장소를 끼워 넣어도 쓸 수 있게.
 * `/api` 하위는 permitAll 이라 access 토큰을 직접 강제한다.
 */
@RequiresAppFeature(AppFeature.COURSE_TRACING)
@RestController
class CourseTraceController(
    private val courseTraceUseCase: CourseTraceUseCase,
) {
    /** 따라가기 시작 — 진행 중 세션이 있으면 그 세션(체크인 포함)을 돌려준다(이어가기). */
    @PostMapping("/api/v1/courses/{courseId}/traces")
    @AccessTokenRequired
    fun start(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
    ): ApiResponse<CourseTraceResponse> =
        ApiResponse.success(
            CourseTraceResponse.from(courseTraceUseCase.start(userId, courseId)),
            "따라가기를 시작했습니다.",
        )

    /** 장소 체크인 — 방문 즉시 한 건씩 기록한다. 같은 장소 재전송은 200. */
    @PostMapping("/api/v1/traces/{traceId}/places")
    @AccessTokenRequired
    fun checkIn(
        @CurrentUserId userId: Long,
        @PathVariable traceId: Long,
        @Valid @RequestBody request: CheckInPlaceRequest,
    ): ApiResponse<Nothing?> {
        courseTraceUseCase.checkIn(
            CheckInPlaceCommand(
                userId = userId,
                traceId = traceId,
                placeId = request.placeId,
                visitedAt = request.visitedAt,
            ),
        )
        return ApiResponse.ok("장소를 방문 처리했습니다.")
    }

    /** 다음 장소까지 도보 경로 — 출발·도착 장소 id 를 받아 Tmap 보행자 경로(거리·시간·폴리라인·안내)를 내려준다. */
    @GetMapping("/api/v1/traces")
    @AccessTokenRequired
    fun route(
        @RequestParam fromPlaceId: Long,
        @RequestParam toPlaceId: Long,
    ): ApiResponse<TraceRouteResponse> =
        ApiResponse.success(TraceRouteResponse.from(courseTraceUseCase.findRoute(fromPlaceId, toPlaceId)))

    /** 따라가기 종료 — 소요 시간·이동 거리 기록 + 미전송 체크인 병합 + tracings_cnt +1. */
    @PostMapping("/api/v1/traces/{traceId}/end")
    @AccessTokenRequired
    fun end(
        @CurrentUserId userId: Long,
        @PathVariable traceId: Long,
        @Valid @RequestBody request: EndCourseTraceRequest,
    ): ApiResponse<Nothing?> {
        courseTraceUseCase.end(
            EndCourseTraceCommand(
                userId = userId,
                traceId = traceId,
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
