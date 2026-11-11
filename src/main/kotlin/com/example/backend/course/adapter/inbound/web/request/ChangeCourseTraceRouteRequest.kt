package com.example.backend.course.adapter.inbound.web.request

import jakarta.validation.constraints.Size

internal const val MAX_TRACE_ROUTE_PLACES = 20

/** 따라가는 중 루트 변경 요청 — 앞으로 갈 장소만 따라갈 순서대로. 이미 체크인한 방문은 보내지 않는다(다시 갈 장소면 담을 수 있다). */
data class ChangeCourseTraceRouteRequest(
    @field:Size(max = MAX_TRACE_ROUTE_PLACES)
    val placeIds: List<Long>,
)
