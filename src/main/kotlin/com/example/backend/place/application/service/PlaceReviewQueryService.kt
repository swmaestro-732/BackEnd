package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceReviewQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceReviewsQuery
import com.example.backend.place.application.port.inbound.dto.PlaceReviewsResult
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceReviewCursor
import com.example.backend.place.application.port.outbound.PlaceReviewQueryPort
import com.example.backend.place.application.port.outbound.PlaceReviewRow
import com.example.backend.place.domain.model.PlaceReviewTag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 별점 표기 범위 — 화면의 별 5개(5~1점 분포를 항상 이 순서로 채운다). */
private val RATING_RANGE = 5 downTo 1

/** 장소 리뷰 목록 조회 유스케이스 */
@Service
@Transactional(readOnly = true)
class PlaceReviewQueryService(
    private val placeReviewQueryPort: PlaceReviewQueryPort,
    private val placeQueryPort: PlaceQueryPort,
) : PlaceReviewQueryUseCase {
    override fun getReviews(query: PlaceReviewsQuery): PlaceReviewsResult {
        val cursor = PlaceReviewCursorCodec.decode(query.cursor)

        // hasNext 판정을 위해 한 개 더 조회한 뒤, 페이지 크기만큼 잘라낸다.
        val rows =
            placeReviewQueryPort.findReviewsByPlace(
                placeId = query.placeId,
                sort = query.sort,
                descending = query.descending,
                cursor = cursor,
                limit = query.size + 1,
            )
        val hasNext = rows.size > query.size
        val page = rows.take(query.size)

        val reviewIds = page.map { it.id }
        val photoUrls = placeReviewQueryPort.findPhotoUrls(reviewIds)
        val tags = placeReviewQueryPort.findTags(reviewIds)
        val ratingCounts = placeReviewQueryPort.countReviewsByRating(query.placeId)
        val photoCount = placeReviewQueryPort.countPhotosByPlace(query.placeId)
        val place = placeQueryPort.findPlaceById(query.placeId)
        val nextCursor =
            if (hasNext) page.lastOrNull()?.let { PlaceReviewCursorCodec.encode(it.toCursor()) } else null

        return PlaceReviewsResult(
            averageRating = place?.averageRating ?: 0.0,
            totalCount = place?.ratingCnt ?: 0,
            ratingDistribution = ratingCounts.toRatingDistribution(),
            photoCount = photoCount.toInt(),
            nextCursor = nextCursor,
            hasNext = hasNext,
            reviews =
                page.map { row ->
                    row.toItem(
                        photoUrls = photoUrls[row.id].orEmpty(),
                        tags = tags[row.id].orEmpty(),
                    )
                },
        )
    }

    private fun PlaceReviewRow.toItem(
        photoUrls: List<String>,
        tags: List<PlaceReviewTag>,
    ) = PlaceReviewsResult.PlaceReviewItem(
        id = id,
        userId = userId,
        rating = rating,
        content = content,
        createdAt = createdAt,
        photoUrls = photoUrls,
        tags = tags.map { it.toTag() },
    )

    private fun Map<Int, Long>.toRatingDistribution(): List<PlaceReviewsResult.RatingCount> =
        RATING_RANGE.map { rating ->
            PlaceReviewsResult.RatingCount(
                rating = rating,
                count = (this[rating] ?: 0L).toInt(),
            )
        }

    private fun PlaceReviewRow.toCursor() = PlaceReviewCursor(rating = rating, createdAt = createdAt, id = id)

    private fun PlaceReviewTag.toTag() = PlaceReviewsResult.Tag(code = code, label = label, icon = icon)
}
