package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseSummary
import java.time.Instant

/**
 * 임시저장 코스 목록 한 건 — `GET /api/v1/courses/drafts`.
 * 포트 DTO([CourseSummary])를 그대로 내리지 않고 응답 계약을 따로 둔다. [CourseSummary] 에 홈 카드용 필드
 * (placeCount, walkingMinutes)가 늘어도 이 API 응답은 바뀌지 않게 하려는 것이다(구버전 앱 호환).
 */
data class CourseDraftResponse(
    val id: Long,
    val authorId: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val area: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
) {
    companion object {
        fun from(summary: CourseSummary) =
            CourseDraftResponse(
                id = summary.id,
                authorId = summary.authorId,
                title = summary.title,
                coverImageUrl = summary.coverImageUrl,
                theme = summary.theme,
                area = summary.area,
                likesCnt = summary.likesCnt,
                savesCnt = summary.savesCnt,
                createdAt = summary.createdAt,
            )

        /** `?mock=true` 폴백 응답. 실구현 전환 시 호출부와 함께 제거한다. */
        val MOCK: List<CourseDraftResponse> =
            listOf(
                CourseDraftResponse(
                    id = 2,
                    authorId = 1,
                    title = "비 오는 날 성수 감성 카페 코스",
                    coverImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600",
                    theme = null,
                    area = null,
                    likesCnt = 0,
                    savesCnt = 0,
                    createdAt = Instant.parse("2026-07-20T02:30:00Z"),
                ),
            )
    }
}
