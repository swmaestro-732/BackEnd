package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseReviewQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseReviewsQuery
import com.example.backend.course.application.port.inbound.dto.CourseReviewsResult
import com.example.backend.course.application.port.outbound.CourseReviewCursor
import com.example.backend.course.application.port.outbound.CourseReviewQueryPort
import com.example.backend.course.application.port.outbound.CourseReviewRow
import com.example.backend.course.domain.model.CourseReviewTag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 별점 표기 범위 — 화면의 별 5개(5~1점 분포를 항상 이 순서로 채운다). */
private val RATING_RANGE = 5 downTo 1

/**
 * 코스 리뷰 목록 조회 유스케이스.
 * 없는 코스는 404 가 아니라 빈 목록(집계 0)이다 — 존재 판정은 상세 화면 몫(장소 리뷰와 같은 규칙).
 */
@Service
@Transactional(readOnly = true)
class CourseReviewQueryService(
    private val courseReviewQueryPort: CourseReviewQueryPort,
) : CourseReviewQueryUseCase {
    override fun getReviews(query: CourseReviewsQuery): CourseReviewsResult {
        val cursor = CourseReviewCursorCodec.decode(query.cursor)

        // hasNext 판정을 위해 한 개 더 조회한 뒤, 페이지 크기만큼 잘라낸다.
        val rows =
            courseReviewQueryPort.findReviewsByCourse(
                courseId = query.courseId,
                sort = query.sort,
                descending = query.descending,
                cursor = cursor,
                limit = query.size + 1,
            )
        val hasNext = rows.size > query.size
        val page = rows.take(query.size)

        val reviewIds = page.map { it.id }
        val photoUrls = courseReviewQueryPort.findPhotoUrls(reviewIds)
        val tags = courseReviewQueryPort.findTags(reviewIds)
        val ratingCounts = courseReviewQueryPort.countReviewsByRating(query.courseId)
        val photoCount = courseReviewQueryPort.countPhotosByCourse(query.courseId)
        val counters = courseReviewQueryPort.findRatingCounters(query.courseId)
        val nextCursor =
            if (hasNext) page.lastOrNull()?.let { CourseReviewCursorCodec.encode(it.toCursor()) } else null

        return CourseReviewsResult(
            averageRating = counters?.averageRating ?: 0.0,
            totalCount = counters?.ratingCnt ?: 0,
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

    private fun CourseReviewRow.toItem(
        photoUrls: List<String>,
        tags: List<CourseReviewTag>,
    ) = CourseReviewsResult.CourseReviewItem(
        id = id,
        userId = userId,
        rating = rating,
        content = content,
        createdAt = createdAt,
        photoUrls = photoUrls,
        tags = tags.map { it.toTag() },
    )

    private fun Map<Int, Long>.toRatingDistribution(): List<CourseReviewsResult.RatingCount> =
        RATING_RANGE.map { rating ->
            CourseReviewsResult.RatingCount(
                rating = rating,
                count = (this[rating] ?: 0L).toInt(),
            )
        }

    private fun CourseReviewRow.toCursor() = CourseReviewCursor(rating = rating, createdAt = createdAt, id = id)

    private fun CourseReviewTag.toTag() = CourseReviewsResult.Tag(code = code, label = label, icon = icon)
}
