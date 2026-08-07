package com.example.backend.direction.adapter.inbound.web.response

/**
 * 도보 시간 응답.
 *
 * - [segments]: 연속한 지점쌍별 도보 소요(분).
 *   - 양수: 도보 이동 가능.
 *   - -1: 도보 불가(서비스 불가 구간·1시간 초과) — "걸어갈 수 없는 거리".
 *   - null: 산출 불가(일시적 오류) — "모름"(도보 불가와 구분).
 *   예: [10, -1, null, 8].
 */
data class WalkingResponse(
    val segments: List<Int?>,
) {
    companion object {
        fun from(segments: List<Int?>): WalkingResponse = WalkingResponse(segments = segments)
    }
}
