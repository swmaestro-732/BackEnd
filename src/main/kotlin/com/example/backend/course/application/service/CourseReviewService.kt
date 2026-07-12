package com.example.backend.course.application.service

import com.example.backend.course.application.dto.CourseReviewPageResult
import com.example.backend.course.application.dto.CourseReviewQuery
import com.example.backend.course.application.dto.CourseReviewResult
import com.example.backend.course.application.dto.CourseReviewSort
import com.example.backend.course.application.dto.CourseReviewTagResult
import com.example.backend.course.application.dto.RatingCountResult
import com.example.backend.course.application.dto.SortDirection
import com.example.backend.course.application.port.inbound.CourseReviewUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import kotlin.math.round

/**
 * 유스케이스 구현 — 코스 리뷰 목록(정렬·페이지네이션).
 *
 * MOCK(SCRUM-223): 아직 아웃바운드 포트/영속성 어댑터가 없어 고정 데이터를 정렬·슬라이스해서 반환한다.
 * 실제 구현 시 정렬/페이징은 DB 쿼리(ORDER BY + LIMIT/OFFSET)로 내려보내고 이 하드코딩만 교체한다.
 * 존재하지 않는 코스는 빈 페이지를 반환한다(코스 존재 검증은 코스 상세 조회의 책임).
 */
@Service
@Transactional(readOnly = true)
class CourseReviewService : CourseReviewUseCase {
    override fun getReviews(
        courseId: Long,
        query: CourseReviewQuery,
    ): CourseReviewPageResult {
        val all = MOCK_REVIEWS[courseId] ?: emptyList()
        val sorted = all.sortedWith(comparator(query.sort, query.order))

        val startIndex = startIndexAfter(query.cursor, sorted)
        val pageItems =
            if (startIndex >= sorted.size) {
                emptyList()
            } else {
                sorted.subList(startIndex, minOf(startIndex + query.size, sorted.size))
            }

        val hasNext = startIndex + pageItems.size < sorted.size
        return CourseReviewPageResult(
            averageRating = averageRating(all),
            totalCount = all.size,
            ratingDistribution = ratingDistribution(all),
            hasCompletedCourse = MOCK_HAS_COMPLETED_COURSE,
            nextCursor = if (hasNext && pageItems.isNotEmpty()) encodeCursor(pageItems.last().id) else null,
            hasNext = hasNext,
            reviews = pageItems,
        )
    }

    /** 별점별 개수. 5~1점 순으로 항상 5개 항목을 만든다(없으면 count=0). */
    private fun ratingDistribution(reviews: List<CourseReviewResult>): List<RatingCountResult> =
        (5 downTo 1).map { star -> RatingCountResult(star, reviews.count { it.rating == star }) }

    /** 정렬 기준·방향으로 비교자를 만든다. 동점(같은 기준값)은 항상 최신순, 그다음 id 로 안정 정렬한다. */
    private fun comparator(
        sort: CourseReviewSort,
        order: SortDirection,
    ): Comparator<CourseReviewResult> {
        val base: Comparator<CourseReviewResult> =
            when (sort) {
                CourseReviewSort.LATEST -> compareBy { it.createdAt }
                CourseReviewSort.RATING -> compareBy { it.rating }
            }
        val directed = if (order == SortDirection.DESC) base.reversed() else base
        return directed.thenByDescending { it.createdAt }.thenByDescending { it.id }
    }

    /** 커서가 가리키는 리뷰의 다음 위치. 커서가 없으면 0, 정렬 결과에 없는 커서면 400. */
    private fun startIndexAfter(
        cursor: String?,
        sorted: List<CourseReviewResult>,
    ): Int {
        if (cursor == null) return 0
        val cursorId = decodeCursor(cursor)
        val index = sorted.indexOfFirst { it.id == cursorId }
        require(index >= 0) { "유효하지 않은 커서입니다." }
        return index + 1
    }

    private fun averageRating(reviews: List<CourseReviewResult>): Double {
        if (reviews.isEmpty()) return 0.0
        return round(reviews.sumOf { it.rating }.toDouble() / reviews.size * 10) / 10
    }

    private fun encodeCursor(reviewId: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(reviewId.toByteArray())

    private fun decodeCursor(cursor: String): String =
        try {
            String(Base64.getUrlDecoder().decode(cursor))
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("유효하지 않은 커서입니다.", e)
        }

    private companion object {
        /** MOCK: 인증(조회자)이 붙기 전 고정값. 실제 구현 시 tracing_course 로 조회자별 완주 여부를 조회한다. */
        const val MOCK_HAS_COMPLETED_COURSE = true

        private fun kstReview(
            id: String,
            authorId: Long,
            rating: Int,
            content: String,
            createdAt: OffsetDateTime,
            relativeTime: String,
            photoUrls: List<String> = emptyList(),
            tags: List<CourseReviewTagResult> = emptyList(),
        ) = CourseReviewResult(id, authorId, rating, content, createdAt, relativeTime, photoUrls, tags)

        private fun tag(
            label: String,
            icon: String,
        ) = CourseReviewTagResult(label, icon)

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

        /** id=1 만 리뷰 데이터, 나머지는 빈 목록 */
        val MOCK_REVIEWS: Map<Long, List<CourseReviewResult>> =
            mapOf(
                1L to
                    listOf(
                        kstReview(
                            id = "1",
                            authorId = 2L,
                            rating = 5,
                            content =
                                "비 오는 날 딱이에요. 통창 자리 순서대로 도니 동선도 완벽했어요. 웨이팅도 거의 없었어요 🌧️",
                            createdAt = kst(2026, 7, 7, 13, 20),
                            relativeTime = "4일 전",
                            photoUrls =
                                listOf(
                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw&s=10",
                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw&s=10",
                                ),
                            tags = listOf(PACKED, SMOOTH),
                        ),
                        kstReview(
                            id = "2",
                            authorId = 3L,
                            rating = 4,
                            content = "코스 좋아요! 세 번째 카페가 조금 붐볐어요.",
                            createdAt = kst(2026, 7, 5, 18, 5),
                            relativeTime = "6일 전",
                            tags = listOf(COMBO),
                        ),
                        kstReview(
                            id = "3",
                            authorId = 4L,
                            rating = 5,
                            content = "사진 찍기 좋은 곳만 모아놨네요. 데이트로 최고였어요.",
                            createdAt = kst(2026, 7, 3, 11, 0),
                            relativeTime = "8일 전",
                            photoUrls =
                                listOf(
                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTHIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA&s=10",
                                ),
                            tags = listOf(COMBO, PACKED),
                        ),
                        kstReview(
                            id = "4",
                            authorId = 5L,
                            rating = 3,
                            content = "코스 자체는 좋은데 주말엔 사람이 너무 많아요.",
                            createdAt = kst(2026, 7, 1, 9, 30),
                            relativeTime = "10일 전",
                            tags = listOf(PACKED),
                        ),
                        kstReview(
                            id = "5",
                            authorId = 6L,
                            rating = 4,
                            content = "도보 동선이 편했어요. 팁 남겨주신 거 참고 많이 됐습니다.",
                            createdAt = kst(2026, 6, 28, 20, 15),
                            relativeTime = "13일 전",
                            tags = listOf(EFFICIENT, WALKABLE),
                        ),
                        kstReview(
                            id = "6",
                            authorId = 7L,
                            rating = 5,
                            content = "재방문 의사 100%. 마지막 카페가 진짜 넓고 좋았어요.",
                            createdAt = kst(2026, 6, 25, 12, 0),
                            relativeTime = "16일 전",
                            photoUrls =
                                listOf(
                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA&s=10",
                                ),
                            tags = listOf(SMOOTH, WALKABLE),
                        ),
                    ),
            )
    }
}
