package com.example.backend.mobile.place.application.port.inbound.dto

import com.example.backend.place.application.port.inbound.dto.PlaceReviewsResult
import com.example.backend.user.application.port.inbound.UserSummaryUseCase.UserSummary
import java.time.Instant

/**
 * 인바운드 포트 반환 DTO — 후기 전체보기 화면 한 페이지.
 * 리뷰(place)에 작성자 프로필(user)을 병합한 결과라 도메인 API 가 아니라 BFF 가 만든다.
 *
 * 집계([averageRating]·[totalCount]·[ratingDistribution]·[photoCount])는 페이지가 아니라 장소 전체 기준이다.
 */
data class PlaceReviewScreenResult(
    val averageRating: Double,
    val totalCount: Int,
    val ratingDistribution: List<RatingCount>,
    val photoCount: Int,
    /** 조회자가 이 장소를 방문했는지 — 리뷰 작성 버튼 노출용. 방문 처리가 아직 모킹이라 고정값이다. */
    val hasVisitedPlace: Boolean,
    val nextCursor: String?,
    val hasNext: Boolean,
    val reviews: List<ReviewItem>,
) {
    companion object {
        /** 탈퇴·미존재 작성자의 대체 닉네임 — 리뷰 자체는 남으므로 카드가 비지 않게 채운다. */
        private const val UNKNOWN_AUTHOR_NICKNAME = "알 수 없음"

        /** 도메인 리뷰 페이지([PlaceReviewsResult])에 작성자 프로필을 병합한다. */
        fun of(
            page: PlaceReviewsResult,
            authors: Map<Long, UserSummary>,
            hasVisitedPlace: Boolean,
        ) = PlaceReviewScreenResult(
            averageRating = page.averageRating,
            totalCount = page.totalCount,
            ratingDistribution = page.ratingDistribution.map { RatingCount(it.rating, it.count) },
            photoCount = page.photoCount,
            hasVisitedPlace = hasVisitedPlace,
            nextCursor = page.nextCursor,
            hasNext = page.hasNext,
            reviews =
                page.reviews.map { review ->
                    val author = authors[review.userId]
                    ReviewItem(
                        id = review.id,
                        author =
                            Author(
                                id = review.userId,
                                nickname = author?.nickname ?: UNKNOWN_AUTHOR_NICKNAME,
                                profileImageUrl = author?.profileImageUrl,
                            ),
                        rating = review.rating,
                        content = review.content,
                        createdAt = review.createdAt,
                        photoUrls = review.photoUrls,
                        tags = review.tags.map { Tag(code = it.code, label = it.label, icon = it.icon) },
                    )
                },
        )
    }

    data class RatingCount(
        val rating: Int,
        val count: Int,
    )

    /** 리뷰 한 건. [content] 는 별점만 남긴 리뷰라면 null 이다. */
    data class ReviewItem(
        val id: Long,
        val author: Author,
        val rating: Int,
        val content: String?,
        val createdAt: Instant,
        val photoUrls: List<String>,
        val tags: List<Tag>,
    )

    /** 작성자 표시 정보. 탈퇴한 사용자는 대체 닉네임으로 채운다. */
    data class Author(
        val id: Long,
        val nickname: String,
        val profileImageUrl: String?,
    )

    data class Tag(
        val code: String,
        val label: String,
        val icon: String,
    )
}
