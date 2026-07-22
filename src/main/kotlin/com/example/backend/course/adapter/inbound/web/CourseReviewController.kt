package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.CourseReviewListResponse
import com.example.backend.course.adapter.inbound.web.response.CourseReviewResponse
import com.example.backend.course.adapter.inbound.web.response.CourseReviewTagResponse
import com.example.backend.course.adapter.inbound.web.response.RatingCountResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 인바운드 어댑터 — 코스 리뷰 목록 조회. **모킹 API**.
 * 쿼리 파라미터는 API 계약 확인용으로 받기만 하고, 정렬·커서·페이지네이션 동작 없이
 * 항상 [MOCK_REVIEWS] 전체를 **고정 응답**(nextCursor=null·hasNext=false)으로 내려준다.
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 *
 * 쿼리 파라미터(모두 받기만 하고 응답에 영향 없음)
 * - sort: LATEST(작성일, 기본) | RATING(평점).
 * - order: ASC(오름차순, 기본) | DESC(내림차순).
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략).
 * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
 * - mockError: 모킹 에러 주입(예: `?mockError=4041`).
 */
@RestController
@RequestMapping("/api/v1")
class CourseReviewController {
    @GetMapping("/courses/{courseId}/reviews")
    fun getReviews(
        @PathVariable courseId: Long,
        @RequestParam(required = false) sort: CourseReviewSort = CourseReviewSort.LATEST,
        @RequestParam(required = false) order: SortDirection = SortDirection.ASC,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseReviewListResponse> {
        MockErrors.throwIfRequested(mockError)

        return ApiResponse.success(
            CourseReviewListResponse(
                averageRating = 4.3,
                totalCount = MOCK_REVIEWS.size,
                ratingDistribution = MOCK_RATING_DISTRIBUTION,
                hasCompletedCourse = MOCK_HAS_COMPLETED_COURSE,
                nextCursor = null,
                hasNext = false,
                reviews = MOCK_REVIEWS,
            ),
        )
    }

    private companion object {
        /** MOCK: 인증(조회자)이 붙기 전 고정값. 실제 구현 시 tracing_course 로 조회자별 완주 여부를 조회한다. */
        const val MOCK_HAS_COMPLETED_COURSE = true

        fun image(token: String) = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc$token&s=10"

        private fun review(
            id: String,
            authorId: Long,
            rating: Int,
            content: String,
            createdAt: OffsetDateTime,
            relativeTime: String,
            photoUrls: List<String> = emptyList(),
            tags: List<CourseReviewTagResponse> = emptyList(),
        ) = CourseReviewResponse(id, authorId, rating, content, createdAt, relativeTime, photoUrls, tags)

        private fun tag(
            label: String,
            icon: String,
        ) = CourseReviewTagResponse(label, icon)

        // 코스 리뷰 태그 카탈로그(icon 키워드 → label). 실제 구현 시 course_review_tags 행에서 온다.
        private val PACKED = tag("구성이 알차요", "packed")
        private val COMBO = tag("장소 조합이 좋아요", "combo")
        private val SMOOTH = tag("흐름이 자연스러워요", "smooth")
        private val EFFICIENT = tag("동선이 효율적이에요", "efficient")
        private val WALKABLE = tag("도보로 다니기 좋아요", "walkable")

        private fun kst(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
        ) = OffsetDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.ofHours(9))

        /** 별점별 개수(5~1점) — 고정 목록([MOCK_REVIEWS]) 기준. */
        val MOCK_RATING_DISTRIBUTION: List<RatingCountResponse> =
            listOf(
                RatingCountResponse(5, 3),
                RatingCountResponse(4, 2),
                RatingCountResponse(3, 1),
                RatingCountResponse(2, 0),
                RatingCountResponse(1, 0),
            )

        /** 코스 리뷰 고정 목록 — 최신순 예시(디자인 · 코스 상세 리뷰). */
        val MOCK_REVIEWS: List<CourseReviewResponse> =
            listOf(
                review(
                    id = "1",
                    authorId = 2L,
                    rating = 5,
                    content =
                        "비 오는 날 딱이에요. 통창 자리 순서대로 도니 동선도 완벽했어요. 웨이팅도 거의 없었어요 🌧️",
                    createdAt = kst(2026, 7, 7, 13, 20),
                    relativeTime = "4일 전",
                    photoUrls =
                        listOf(
                            image("Qri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw"),
                            image("R_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw"),
                        ),
                    tags = listOf(PACKED, SMOOTH),
                ),
                review(
                    id = "2",
                    authorId = 3L,
                    rating = 4,
                    content = "코스 좋아요! 세 번째 카페가 조금 붐볐어요.",
                    createdAt = kst(2026, 7, 5, 18, 5),
                    relativeTime = "6일 전",
                    tags = listOf(COMBO),
                ),
                review(
                    id = "3",
                    authorId = 4L,
                    rating = 5,
                    content = "사진 찍기 좋은 곳만 모아놨네요. 데이트로 최고였어요.",
                    createdAt = kst(2026, 7, 3, 11, 0),
                    relativeTime = "8일 전",
                    photoUrls = listOf(image("THIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA")),
                    tags = listOf(COMBO, PACKED),
                ),
                review(
                    id = "4",
                    authorId = 5L,
                    rating = 3,
                    content = "코스 자체는 좋은데 주말엔 사람이 너무 많아요.",
                    createdAt = kst(2026, 7, 1, 9, 30),
                    relativeTime = "10일 전",
                    tags = listOf(PACKED),
                ),
                review(
                    id = "5",
                    authorId = 6L,
                    rating = 4,
                    content = "도보 동선이 편했어요. 팁 남겨주신 거 참고 많이 됐습니다.",
                    createdAt = kst(2026, 6, 28, 20, 15),
                    relativeTime = "13일 전",
                    tags = listOf(EFFICIENT, WALKABLE),
                ),
                review(
                    id = "6",
                    authorId = 7L,
                    rating = 5,
                    content = "재방문 의사 100%. 마지막 카페가 진짜 넓고 좋았어요.",
                    createdAt = kst(2026, 6, 25, 12, 0),
                    relativeTime = "16일 전",
                    photoUrls = listOf(image("Qr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA")),
                    tags = listOf(SMOOTH, WALKABLE),
                ),
            )
    }
}
