package com.example.backend.mobile.home.application.port.outbound.dto

import java.time.Instant

/** BFF 아웃바운드 출력 — 진행 중 따라가기 세션과 코스 요약(course 도메인 응답을 BFF 안으로 복사한 격리 DTO). */
data class HomeTracingCourse(
    val traceId: Long,
    val courseId: Long,
    val title: String,
    val coverImageUrl: String?,
    val area: String?,
    val placeCount: Int,
    val startedAt: Instant,
)
