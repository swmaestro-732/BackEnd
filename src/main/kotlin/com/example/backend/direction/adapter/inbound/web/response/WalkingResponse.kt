package com.example.backend.direction.adapter.inbound.web.response

/**
 * 도보 시간 응답.
 *
 * - [segments]: 연속한 지점쌍별 도보 소요(분). 산출 불가 구간은 null.
 */
data class WalkingResponse(
    val segments: List<Int?>,
) {
    companion object {
        fun from(segments: List<Int?>): WalkingResponse = WalkingResponse(segments = segments)
    }
}
