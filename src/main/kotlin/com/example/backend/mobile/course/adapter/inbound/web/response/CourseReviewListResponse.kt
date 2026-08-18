package com.example.backend.mobile.course.adapter.inbound.web.response

import java.time.Instant

/**
 * 웹 응답 DTO — 코스 후기 전체보기 **화면 조합**(BFF) 한 페이지(**모킹 API**).
 * averageRating/totalCount/ratingDistribution/photoCount 는 코스 전체 집계,
 * nextCursor/hasNext 는 커서 페이지 메타다.
 *
 * 리뷰(course) + **작성자 프로필**(user)을 함께 내려주므로 도메인 API 가 아니라 BFF 가 담당한다
 * — 화면이 작성자 닉네임·프로필 이미지를 그리는데 도메인 코스 API 범위를 넘는다(api-design.md 원칙).
 * 필드 근거는 디자인(밴드 I · 5f 후기 전체보기 — 평균 평점 + 별점 분포 + 후기 사진 개수 +
 * 최신순/높은 평점 정렬 + 작성자 닉네임·별점·상대시간·태그 칩)이다.
 * 장소 후기 목록([com.example.backend.mobile.place.adapter.inbound.web.response.PlaceReviewListResponse])과
 * 같은 모양으로 맞췄다 — 두 후기 화면이 같은 구성이라 프론트가 컴포넌트를 공유할 수 있게 했다.
 *
 * 코스 상세 시트의 리뷰 미리보기([ReviewPreviewResponse])와는 DTO 가 갈라져 있다 —
 * 미리보기는 태그에 code 가 없고 작성자 프로필이 논널이라, 통합은 실구현 때 함께 정리한다.
 */
data class CourseReviewListResponse(
    val averageRating: Double,
    val totalCount: Int,
    val ratingDistribution: List<RatingCountResponse>,
    /** 후기에 달린 사진 총 개수 — 디자인 "후기 사진 86 · 모두 보기" 헤더. */
    val photoCount: Int,
    /** 조회자가 이 코스를 완주(트레이싱 완료)했는지. 리뷰 작성 가능 여부 노출 등에 사용. */
    val hasCompletedCourse: Boolean,
    val nextCursor: String?,
    val hasNext: Boolean,
    val reviews: List<CourseReviewItemResponse>,
) {
    companion object {
        /** MOCK: 조회자별 완주 이력 조회 전 고정값. 실제 구현 시 tracing_course 로 조회자별 완주 여부를 조회한다. */
        private const val MOCK_HAS_COMPLETED_COURSE = true

        /** 목 사진은 실제로 열리는 이미지를 쓴다 — 프론트가 목만으로 후기 사진 그리드를 그려볼 수 있게. */
        private fun image(token: String) = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc$token&s=10"

        private fun author(
            id: Long,
            nickname: String,
            profileImageUrl: String? = null,
        ) = CourseReviewAuthorResponse(id, nickname, profileImageUrl)

        private fun review(
            id: Long,
            author: CourseReviewAuthorResponse,
            rating: Int,
            content: String,
            createdAt: Instant,
            photoUrls: List<String> = emptyList(),
            tags: List<CourseReviewTagResponse> = emptyList(),
        ) = CourseReviewItemResponse(id, author, rating, content, createdAt, photoUrls, tags)

        // 코스 리뷰 태그 카탈로그(코드 → 문구·이모지). `.ai/taxonomy.md` "코스 리뷰 태그"가 정본이며,
        // 실제 구현 시 course_review_tags 마스터 행에서 온다.
        private val PACKED = CourseReviewTagResponse("packed", "구성이 알차요", "📦")
        private val COMBO = CourseReviewTagResponse("combo", "장소 조합이 좋아요", "🌿")
        private val SMOOTH = CourseReviewTagResponse("smooth", "흐름이 자연스러워요", "🌊")
        private val EFFICIENT = CourseReviewTagResponse("efficient", "동선이 효율적이에요", "🎢")
        private val WALKABLE = CourseReviewTagResponse("walkable", "도보로 다니기 좋아요", "👣")
        private val DATE = CourseReviewTagResponse("date", "데이트 코스로 좋아요", "❤️")
        private val MEMORABLE = CourseReviewTagResponse("memorable", "기억에 남아요", "🔖")
        private val REVISIT = CourseReviewTagResponse("revisit", "또 가고 싶어요", "🔁")

        /** 코스 리뷰 고정 목록 — 최신순 예시(디자인 · 5f "따라간 후기" · 비 오는 날 성수 카페 코스). */
        private fun mockReviews(): List<CourseReviewItemResponse> =
            listOf(
                review(
                    id = 1,
                    author = author(10, "소마님", "https://cdn.example.com/users/10.jpg"),
                    rating = 5,
                    content = "비 오는 날 딱이에요. 통창 자리 순서대로 도니 동선도 완벽했어요. 웨이팅도 거의 없었어요 🌧️",
                    createdAt = Instant.parse("2026-08-16T04:20:00Z"),
                    photoUrls =
                        listOf(
                            image("Qri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw"),
                            image("R_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw"),
                        ),
                    tags = listOf(PACKED, SMOOTH),
                ),
                review(
                    id = 2,
                    author = author(11, "현우"),
                    rating = 4,
                    content = "대림창고 웨이팅이 좀 있었지만 전반적으로 만족스러운 코스! 마지막 와인바가 특히 좋았어요.",
                    createdAt = Instant.parse("2026-08-13T09:05:00Z"),
                    tags = listOf(COMBO),
                ),
                review(
                    id = 3,
                    author = author(12, "지은", "https://cdn.example.com/users/12.jpg"),
                    rating = 5,
                    content = "데이트 코스로 완벽했어요. 사진 찍을 곳도 많고 이동 동선도 편했습니다 :)",
                    createdAt = Instant.parse("2026-08-11T02:00:00Z"),
                    photoUrls = listOf(image("THIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA")),
                    tags = listOf(DATE, MEMORABLE),
                ),
                review(
                    id = 4,
                    author = author(13, "성수산책"),
                    rating = 3,
                    content = "코스 자체는 좋은데 주말엔 사람이 너무 많아요.",
                    createdAt = Instant.parse("2026-08-08T00:30:00Z"),
                    tags = listOf(PACKED),
                ),
                review(
                    id = 5,
                    author = author(14, "우디"),
                    rating = 4,
                    content = "도보 동선이 편했어요. 팁 남겨주신 거 참고 많이 됐습니다.",
                    createdAt = Instant.parse("2026-08-05T11:15:00Z"),
                    tags = listOf(EFFICIENT, WALKABLE),
                ),
                review(
                    id = 6,
                    author = author(15, "빵순이", "https://cdn.example.com/users/15.jpg"),
                    rating = 5,
                    content = "재방문 의사 100%. 마지막 카페가 진짜 넓고 좋았어요.",
                    createdAt = Instant.parse("2026-08-01T03:00:00Z"),
                    photoUrls = listOf(image("Qr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA")),
                    tags = listOf(SMOOTH, REVISIT),
                ),
            )

        /** 별점별 개수(5~1점) — 고정 목록([mockReviews]) 기준. */
        private fun ratingDistributionOf(reviews: List<CourseReviewItemResponse>): List<RatingCountResponse> =
            (5 downTo 1).map { rating -> RatingCountResponse(rating, reviews.count { it.rating == rating }) }

        /**
         * 코스 후기 목록 목 — 항상 [mockReviews] 전체를 고정 응답(nextCursor=null·hasNext=false)으로 내려준다.
         * 집계 값(평균·개수·분포·사진 수)은 목록에서 계산해 서로 어긋나지 않게 한다.
         */
        fun mock(): CourseReviewListResponse {
            val reviews = mockReviews()
            return CourseReviewListResponse(
                averageRating = Math.round(reviews.sumOf { it.rating } * 10.0 / reviews.size) / 10.0,
                totalCount = reviews.size,
                ratingDistribution = ratingDistributionOf(reviews),
                photoCount = reviews.sumOf { it.photoUrls.size },
                hasCompletedCourse = MOCK_HAS_COMPLETED_COURSE,
                nextCursor = null,
                hasNext = false,
                reviews = reviews,
            )
        }
    }
}

/** 상대 시간("2일 전")은 내려주지 않는다 — [createdAt](UTC)으로 클라이언트가 표기한다. */
data class CourseReviewItemResponse(
    val id: Long,
    val author: CourseReviewAuthorResponse,
    val rating: Int,
    val content: String,
    val createdAt: Instant,
    val photoUrls: List<String>,
    val tags: List<CourseReviewTagResponse>,
)

/**
 * 리뷰 작성자 — 간략 정보(닉네임·프로필 이미지). 프로필 이미지는 없을 수 있다.
 * 코스 상세 시트의 [ReviewAuthorResponse] 와 달리 profileImageUrl 이 nullable 이다.
 */
data class CourseReviewAuthorResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
)

/**
 * 리뷰 태그 — code(`.ai/taxonomy.md` 키워드) + label(문구) + icon(이모지).
 * code 는 생성 요청(`tagCodes`)이 쓰는 값과 같아 클라이언트가 선택 상태를 되짚을 수 있다.
 */
data class CourseReviewTagResponse(
    val code: String,
    val label: String,
    val icon: String,
)
