package com.example.backend.course.adapter.inbound.web.request

import jakarta.validation.constraints.Positive
import java.time.Instant

/** 장소 체크인 요청 — 도착(방문 체크) 시각은 UTC. */
data class CheckInPlaceRequest(
    @field:Positive
    val placeId: Long,
    val visitedAt: Instant,
)
