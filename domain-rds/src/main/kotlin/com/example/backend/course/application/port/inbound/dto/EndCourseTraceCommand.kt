package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/** 따라가기 종료 커맨드 — 인바운드 포트([com.example.backend.course.application.port.inbound.CourseTraceUseCase]) 입력. */
data class EndCourseTraceCommand(
    val userId: Long,
    val traceId: Long,
    /** 서버에 못 보낸 체크인이 있으면 여기서 합친다. 이미 체크인한 장소는 무시한다. */
    val visitedPlaces: List<VisitedPlaceCommand>,
    /** 소요 시간(분), 클라이언트 측정값. */
    val durationMinutes: Int,
    /** 이동 거리(m), 클라이언트 측정값. */
    val distanceMeters: Int,
)

/** 장소 체크인 커맨드 — 따라가기 중 장소 하나를 방문 처리한다. */
data class CheckInPlaceCommand(
    val userId: Long,
    val traceId: Long,
    val placeId: Long,
    val visitedAt: Instant,
)

data class VisitedPlaceCommand(
    val placeId: Long,
    val visitedAt: Instant,
)
