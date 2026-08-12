package com.example.backend.user.application.port.inbound.dto

import java.time.Instant

/** 따라가기 시작 결과 — 생성된 tracing id 와 코스·시작 시각. */
data class TracingResult(
    val tracingId: Long,
    val courseId: Long,
    val startedAt: Instant,
)
