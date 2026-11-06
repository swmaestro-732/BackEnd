package com.example.backend.course.adapter.inbound.web.request

import jakarta.validation.constraints.Size

/** 따라가는 중 루트 변경 요청 — 앞으로 갈 장소(아직 체크인하지 않은 곳)만 따라갈 순서대로. 이미 체크인한 장소는 보내지 않는다. */
data class ChangeCourseTraceRouteRequest(
    @field:Size(max = MAX_TRACE_ROUTE_PLACES)
    val placeIds: List<Long>,
)
