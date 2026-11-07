package com.example.backend.course.adapter.inbound.web.request

import jakarta.validation.constraints.Size

internal const val MAX_TRACE_ROUTE_PLACES = 20

/** 따라가기 시작 V2 요청 — 따라갈 장소 id 를 따라갈 순서대로. 코스 장소를 빼거나 순서를 바꾸거나 코스 밖 장소를 넣을 수 있다. */
data class StartCourseTraceRequest(
    @field:Size(min = 1, max = MAX_TRACE_ROUTE_PLACES)
    val placeIds: List<Long>,
)
