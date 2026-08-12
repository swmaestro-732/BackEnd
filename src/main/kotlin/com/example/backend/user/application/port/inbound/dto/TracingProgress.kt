package com.example.backend.user.application.port.inbound.dto

import java.time.Instant

/**
 * 따라가기 진행 상태 — 체크인 장소 수 대비 코스 전체 장소 수, 완주 여부.
 * [checkedPlaces] 가 [totalPlaces] 와 같아지면 자동 완주([completed]=true, [completedAt] 세팅)된다.
 */
data class TracingProgress(
    val tracingId: Long,
    val courseId: Long,
    val totalPlaces: Int,
    val checkedPlaces: Int,
    val completed: Boolean,
    val completedAt: Instant?,
)
