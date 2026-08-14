package com.example.backend.place.adapter.inbound.web.response

import java.time.Duration
import java.time.Instant

/**
 * 웹 응답 DTO — 장소 리뷰 한 페이지(**모킹 API**).
 * averageRating/totalCount/ratingDistribution/photoCount 는 장소 전체 집계,
 * nextCursor/hasNext 는 커서 페이지 메타다.
 *
 * 노션 명세서(Place · place-review · "장소 리뷰 목록 조회") 본문이 비어 있어 디자인에서 도출했다 —
 * 장소 상세 시트("리뷰 ★ 4.8 · 1,240 · 전체보기", 작성자·별점·상대시간 미리보기)와
 * 후기 전체보기 화면(평균 평점 + 별점 분포 + 후기 사진 개수 + 최신순/높은 평점 정렬).
 */
data class PlaceReviewListResponse(
    val averageRating: Double,
    val totalCount: Int,
    val ratingDistribution: List<PlaceRatingCountResponse>,
    /** 후기에 달린 사진 총 개수 — 디자인 "후기 사진 86 · 모두 보기" 헤더. */
    val photoCount: Int,
    /** 조회자가 이 장소를 방문(저장 장소 방문 처리·따라가기 체크인)했는지. 리뷰 작성 버튼 노출 등에 사용. */
    val hasVisitedPlace: Boolean,
    val nextCursor: String?,
    val hasNext: Boolean,
    val reviews: List<PlaceReviewResponse>,
) {
    companion object {
        /** MOCK: 조회자별 방문 이력 조회 전 고정값. 실제 구현 시 saved_places.visited_at·따라가기 체크인으로 판정한다. */
        private const val MOCK_HAS_VISITED_PLACE = true

        private fun image(name: String) = "https://cdn.example.com/places/101/reviews/$name.jpg"

        private fun author(
            id: Long,
            nickname: String,
            profileImageUrl: String? = null,
        ) = PlaceReviewAuthorResponse(id, nickname, profileImageUrl)

        private fun review(
            id: Long,
            author: PlaceReviewAuthorResponse,
            rating: Int,
            content: String,
            createdAt: Instant,
            photoUrls: List<String> = emptyList(),
            tags: List<PlaceReviewTagResponse> = emptyList(),
        ) = PlaceReviewResponse(id, author, rating, content, createdAt, relativeTimeOf(createdAt), photoUrls, tags)

        /** createdAt 로부터 상대 시간 문자열을 계산한다 — 고정 문자열이 시간이 지나며 어긋나는 것을 막는다. */
        private fun relativeTimeOf(createdAt: Instant): String {
            val minutes = Duration.between(createdAt, Instant.now()).toMinutes().coerceAtLeast(0)
            return when {
                minutes < 1 -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                minutes < 60 * 24 -> "${minutes / 60}시간 전"
                else -> "${minutes / (60 * 24)}일 전"
            }
        }

        // 장소 리뷰 태그 카탈로그(코드 → 문구·이모지). `.ai/taxonomy.md` "장소 리뷰 태그"가 정본이며,
        // 실제 구현 시 place_review_tags 마스터 행에서 온다.
        private val COFFEE = PlaceReviewTagResponse("coffee", "커피가 맛있어요", "☕")
        private val DESSERT = PlaceReviewTagResponse("dessert", "디저트가 맛있어요", "🍰")
        private val SIGNATURE = PlaceReviewTagResponse("signature", "시그니처 메뉴가 좋아요", "🌟")
        private val LINGERING = PlaceReviewTagResponse("lingering", "오래 있기 좋아요", "🕰️")
        private val OUTLETS = PlaceReviewTagResponse("outlets", "콘센트가 많아요", "🔌")
        private val VIEW = PlaceReviewTagResponse("view", "뷰가 좋아요", "🏔️")
        private val PHOTO = PlaceReviewTagResponse("photo", "사진 찍기 좋아요", "📸")
        private val SPACIOUS = PlaceReviewTagResponse("spacious", "공간이 넓어요", "↔️")
        private val NOWAIT = PlaceReviewTagResponse("nowait", "웨이팅이 적어요", "⏳")

        /**
         * 장소 리뷰 고정 목록 — 최신순 예시(디자인 · 장소 상세 시트 "어니언 성수" 리뷰).
         * relativeTime 이 호출 시점 기준으로 계산되도록 매 요청 새로 만든다.
         */
        private fun mockReviews(): List<PlaceReviewResponse> =
            listOf(
                review(
                    id = 1,
                    author = author(10, "현우님", "https://cdn.example.com/users/10.jpg"),
                    rating = 5,
                    content = "팡도르가 정말 맛있어요. 통창 자리 뷰도 최고. 웨이팅은 조금 있었어요.",
                    createdAt = Instant.parse("2026-08-12T05:20:00Z"),
                    photoUrls = listOf(image("1-1"), image("1-2")),
                    tags = listOf(COFFEE, VIEW),
                ),
                review(
                    id = 2,
                    author = author(11, "커피러버"),
                    rating = 4,
                    content = "빵이 다양하고 공간이 넓어요. 오후엔 자리가 금방 차요.",
                    createdAt = Instant.parse("2026-08-10T09:00:00Z"),
                    tags = listOf(DESSERT, SPACIOUS),
                ),
                review(
                    id = 3,
                    author = author(12, "소마님", "https://cdn.example.com/users/12.jpg"),
                    rating = 5,
                    content = "통창 자리에서 마시는 라떼가 진하고 좋았어요. 비 오는 날 분위기 최고!",
                    createdAt = Instant.parse("2026-08-08T02:10:00Z"),
                    photoUrls = listOf(image("3-1")),
                    tags = listOf(COFFEE, PHOTO),
                ),
                review(
                    id = 4,
                    author = author(13, "성수산책"),
                    rating = 3,
                    content = "커피는 좋은데 주말 오후엔 자리가 거의 없어요.",
                    createdAt = Instant.parse("2026-08-05T07:45:00Z"),
                    tags = listOf(COFFEE),
                ),
                review(
                    id = 5,
                    author = author(14, "빵순이", "https://cdn.example.com/users/14.jpg"),
                    rating = 4,
                    content = "베이커리 종류가 많아서 골라 먹는 재미가 있어요.",
                    createdAt = Instant.parse("2026-08-02T11:30:00Z"),
                    tags = listOf(DESSERT, SIGNATURE),
                ),
                review(
                    id = 6,
                    author = author(15, "우디"),
                    rating = 5,
                    content = "평일 오전엔 한산해서 오래 앉아 있기 좋아요. 콘센트도 넉넉합니다.",
                    createdAt = Instant.parse("2026-07-30T01:00:00Z"),
                    photoUrls = listOf(image("6-1")),
                    tags = listOf(LINGERING, OUTLETS, NOWAIT),
                ),
            )

        /** 별점별 개수(5~1점) — 고정 목록([mockReviews]) 기준. */
        private fun ratingDistributionOf(reviews: List<PlaceReviewResponse>): List<PlaceRatingCountResponse> =
            (5 downTo 1).map { rating -> PlaceRatingCountResponse(rating, reviews.count { it.rating == rating }) }

        /**
         * 장소 리뷰 목록 목 — 항상 [mockReviews] 전체를 고정 응답(nextCursor=null·hasNext=false)으로 내려준다.
         * 집계 값(평균·개수·분포·사진 수)은 목록에서 계산해 서로 어긋나지 않게 한다.
         */
        fun mock(): PlaceReviewListResponse {
            val reviews = mockReviews()
            return PlaceReviewListResponse(
                averageRating = Math.round(reviews.sumOf { it.rating } * 10.0 / reviews.size) / 10.0,
                totalCount = reviews.size,
                ratingDistribution = ratingDistributionOf(reviews),
                photoCount = reviews.sumOf { it.photoUrls.size },
                hasVisitedPlace = MOCK_HAS_VISITED_PLACE,
                nextCursor = null,
                hasNext = false,
                reviews = reviews,
            )
        }
    }
}

/** 별점별 리뷰 개수. 5~1점 각각 항상 존재(해당 별점이 없으면 count=0). */
data class PlaceRatingCountResponse(
    val rating: Int,
    val count: Int,
)

data class PlaceReviewResponse(
    val id: Long,
    val author: PlaceReviewAuthorResponse,
    val rating: Int,
    val content: String,
    val createdAt: Instant,
    val relativeTime: String,
    val photoUrls: List<String>,
    val tags: List<PlaceReviewTagResponse>,
)

/**
 * 리뷰 작성자 — 간략 정보(닉네임·프로필 이미지). 프로필 이미지는 없을 수 있다.
 * 후기 목록 화면이 작성자를 그리는데 병합해 줄 BFF·유저 조회 API가 없어 도메인 API가 함께 내린다
 * (장소 상세 BFF `PlaceReviewResponse` 와 같은 모양).
 */
data class PlaceReviewAuthorResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
)

/**
 * 리뷰 태그 — code(`.ai/taxonomy.md` 키워드) + label(문구) + icon(이모지).
 * code 는 생성 요청(`tagCodes`)이 쓰는 값과 같아 클라이언트가 선택 상태를 되짚을 수 있다.
 */
data class PlaceReviewTagResponse(
    val code: String,
    val label: String,
    val icon: String,
)
