package com.example.backend.mobile.place.application.service

import com.example.backend.mobile.place.application.port.inbound.PlaceReviewScreenUseCase
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceReviewScreenQuery
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceReviewScreenResult
import com.example.backend.place.application.port.inbound.PlaceReviewQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceReviewSortKey
import com.example.backend.place.application.port.inbound.dto.PlaceReviewsQuery
import com.example.backend.place.application.port.inbound.dto.PlaceReviewsResult
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 탈퇴·미존재 작성자의 대체 닉네임 — 리뷰 자체는 남으므로 카드가 비지 않게 채운다. */
private const val UNKNOWN_AUTHOR_NICKNAME = "알 수 없음"

/** 후기 전체보기 화면 조합 서비스(BFF). */
@Service
@Transactional(readOnly = true)
class PlaceReviewScreenService(
    private val placeReviewQueryUseCase: PlaceReviewQueryUseCase,
    private val userSummaryUseCase: UserSummaryUseCase,
) : PlaceReviewScreenUseCase {
    override fun getScreen(query: PlaceReviewScreenQuery): PlaceReviewScreenResult {
        val page =
            placeReviewQueryUseCase.getReviews(
                PlaceReviewsQuery(
                    placeId = query.placeId,
                    sort = query.sort,
                    descending = query.descending,
                    cursor = query.cursor,
                    size = query.size,
                ),
            )
        val authors =
            userSummaryUseCase
                .findSummaries(page.reviews.map { it.userId }.distinct())
                .associateBy { it.id }

        return PlaceReviewScreenResult(
            averageRating = page.averageRating,
            totalCount = page.totalCount,
            ratingDistribution =
                page.ratingDistribution.map { PlaceReviewScreenResult.RatingCount(it.rating, it.count) },
            photoCount = page.photoCount,
            hasVisitedPlace = STUB_HAS_VISITED_PLACE,
            nextCursor = page.nextCursor,
            hasNext = page.hasNext,
            reviews =
                page.reviews.map { review ->
                    val author = authors[review.userId]
                    review.toItem(
                        PlaceReviewScreenResult.Author(
                            id = review.userId,
                            nickname = author?.nickname ?: UNKNOWN_AUTHOR_NICKNAME,
                            profileImageUrl = author?.profileImageUrl,
                        ),
                    )
                },
        )
    }

    private fun PlaceReviewsResult.PlaceReviewItem.toItem(author: PlaceReviewScreenResult.Author) =
        PlaceReviewScreenResult.ReviewItem(
            id = id,
            author = author,
            rating = rating,
            content = content,
            createdAt = createdAt,
            photoUrls = photoUrls,
            tags = tags.map { PlaceReviewScreenResult.Tag(code = it.code, label = it.label, icon = it.icon) },
        )

    private companion object {
        /** STUB: 방문 이력 판정 전 고정값(모킹 응답과 같은 값). 방문 처리 실구현 시 제거한다. */
        const val STUB_HAS_VISITED_PLACE = true
    }
}
