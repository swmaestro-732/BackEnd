package com.example.backend.direction.adapter.inbound.web.response

/**
 * 도보 시간 응답.
 *
 * - [segments]: 연속한 지점쌍별 도보 소요(분). 산출 불가 구간은 null.
 * - [totalMinutes]: null 이 아닌 구간의 합. 모든 구간이 null 이면 null.
 */
data class WalkingResponse(
    val segments: List<Int?>,
    val totalMinutes: Int?,
) {
    companion object {
        fun from(segments: List<Int?>): WalkingResponse {
            val present = segments.filterNotNull()
            return WalkingResponse(
                segments = segments,
                totalMinutes = if (present.isEmpty()) null else present.sum(),
            )
        }
    }
}
