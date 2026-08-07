package com.example.backend.direction.adapter.inbound.web.response

/**
 * 도보 시간 응답.
 *
 * - [segments]: 연속한 지점쌍별 도보 소요(분). 도보 불가(에러·서비스 불가 구간·1시간 초과) 구간은 -1. 예: [10, -1, 8].
 */
data class WalkingResponse(
    val segments: List<Int>,
) {
    companion object {
        fun from(segments: List<Int>): WalkingResponse = WalkingResponse(segments = segments)
    }
}
