package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/** 따라가기 종료 커맨드 — 인바운드 포트([com.example.backend.course.application.port.inbound.CourseTraceUseCase]) 입력. */
data class EndCourseTraceCommand(
    val userId: Long,
    val courseId: Long,
    /** 방문(체크)한 장소, 요청 순서대로. */
    val visitedPlaces: List<VisitedPlaceCommand>,
    /** 소요 시간(분), 클라이언트 측정값. */
    val durationMinutes: Int,
    /** 이동 거리(m), 클라이언트 측정값. */
    val distanceMeters: Int,
)

data class VisitedPlaceCommand(
    val placeId: Long,
    val visitedAt: Instant,
)
