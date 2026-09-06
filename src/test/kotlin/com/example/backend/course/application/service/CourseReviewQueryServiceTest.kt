package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.dto.CourseReviewSortKey
import com.example.backend.course.application.port.inbound.dto.CourseReviewsQuery
import com.example.backend.course.application.port.outbound.CourseRatingCounters
import com.example.backend.course.application.port.outbound.CourseReviewCursor
import com.example.backend.course.application.port.outbound.CourseReviewQueryPort
import com.example.backend.course.application.port.outbound.CourseReviewRow
import com.example.backend.course.domain.model.CourseReviewTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant

/**
 * [CourseReviewQueryService] 단위 테스트 — 읽기 포트를 목으로 대체해 페이지 조립 규칙만 검증한다.
 * 검증 대상: size+1 조회의 hasNext/nextCursor 판정, 사진·태그 배치 병합, 분포 5~1점 고정 채움,
 * 카운터 기반 평균·총개수, 없는 코스의 빈 목록(집계 0) 규칙.
 * (커서 인코딩 형식·비정상 커서 방어는 [CourseReviewCursorCodecTest] 가 커버한다.)
 */
class CourseReviewQueryServiceTest {
    private val queryPort = mock(CourseReviewQueryPort::class.java)
    private val service = CourseReviewQueryService(queryPort)

    @Test
    fun `한 페이지를 집계·자식과 함께 조립한다`() {
        // size+1(3)개가 돌아오면 마지막 행은 잘려 나가고 hasNext 판정에만 쓰인다.
        val rows =
            listOf(
                row(id = 3, rating = 5, createdAt = "2026-09-03T00:00:00Z"),
                row(id = 2, rating = 4, createdAt = "2026-09-02T00:00:00Z", content = "동선이 편했어요"),
                row(id = 1, rating = 3, createdAt = "2026-09-01T00:00:00Z"),
            )
        `when`(
            queryPort.findReviewsByCourse(
                courseId = COURSE_ID,
                sort = CourseReviewSortKey.LATEST,
                descending = true,
                cursor = null,
                limit = 3,
            ),
        ).thenReturn(rows)
        `when`(queryPort.findPhotoUrls(listOf(3L, 2L)))
            .thenReturn(mapOf(3L to listOf("https://cdn.example.com/3-1.jpg")))
        `when`(queryPort.findTags(listOf(3L, 2L)))
            .thenReturn(mapOf(3L to listOf(CourseReviewTag.PACKED)))
        `when`(queryPort.countReviewsByRating(COURSE_ID)).thenReturn(mapOf(5 to 1L, 4 to 1L, 3 to 1L))
        `when`(queryPort.countPhotosByCourse(COURSE_ID)).thenReturn(4L)
        `when`(queryPort.findRatingCounters(COURSE_ID))
            .thenReturn(CourseRatingCounters(ratingSum = 12, ratingCnt = 3))

        val result = service.getReviews(CourseReviewsQuery(courseId = COURSE_ID, size = 2))

        assertEquals(4.0, result.averageRating) // 12 / 3 — 카운터 기반이라 페이지와 무관
        assertEquals(3, result.totalCount)
        assertEquals(4, result.photoCount)
        assertTrue(result.hasNext)
        assertEquals(listOf(3L, 2L), result.reviews.map { it.id }) // size 만큼만 잘린다
        // 분포는 5~1점 다섯 칸을 항상 채운다(없는 별점은 0).
        assertEquals(
            listOf(5 to 1, 4 to 1, 3 to 1, 2 to 0, 1 to 0),
            result.ratingDistribution.map {
                it.rating to
                    it.count
            },
        )
        // 사진·태그는 리뷰 id 로 병합되고, 없는 리뷰는 빈 목록이다.
        assertEquals(listOf("https://cdn.example.com/3-1.jpg"), result.reviews[0].photoUrls)
        assertEquals(listOf("packed"), result.reviews[0].tags.map { it.code })
        assertEquals(emptyList<String>(), result.reviews[1].photoUrls)
        assertEquals("동선이 편했어요", result.reviews[1].content)
        // nextCursor 는 페이지 마지막 행(id=2)의 키다.
        assertEquals(
            CourseReviewCursor(rating = 4, createdAt = Instant.parse("2026-09-02T00:00:00Z"), id = 2),
            CourseReviewCursorCodec.decode(result.nextCursor),
        )
    }

    @Test
    fun `마지막 페이지면 hasNext 없이 nextCursor 도 비운다`() {
        `when`(
            queryPort.findReviewsByCourse(
                courseId = COURSE_ID,
                sort = CourseReviewSortKey.LATEST,
                descending = true,
                cursor = null,
                limit = 11,
            ),
        ).thenReturn(listOf(row(id = 1, rating = 5, createdAt = "2026-09-01T00:00:00Z")))
        `when`(queryPort.findRatingCounters(COURSE_ID))
            .thenReturn(CourseRatingCounters(ratingSum = 5, ratingCnt = 1))

        val result = service.getReviews(CourseReviewsQuery(courseId = COURSE_ID))

        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
        assertEquals(listOf(1L), result.reviews.map { it.id })
    }

    @Test
    fun `없는 코스는 404 가 아니라 빈 목록과 집계 0 이다`() {
        // findRatingCounters 가 null(삭제·부재 코스)이어도 예외 없이 0 으로 채운다 — 존재 판정은 상세 화면 몫.
        val result = service.getReviews(CourseReviewsQuery(courseId = 999999L))

        assertEquals(0.0, result.averageRating)
        assertEquals(0, result.totalCount)
        assertEquals(0, result.photoCount)
        assertEquals(emptyList<Long>(), result.reviews.map { it.id })
        assertEquals(listOf(0, 0, 0, 0, 0), result.ratingDistribution.map { it.count })
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    fun `커서 문자열을 디코딩해 포트에 넘긴다`() {
        val cursor = CourseReviewCursor(rating = 4, createdAt = Instant.parse("2026-09-02T00:00:00.123456Z"), id = 2)
        `when`(
            queryPort.findReviewsByCourse(
                courseId = COURSE_ID,
                sort = CourseReviewSortKey.RATING,
                descending = false,
                cursor = cursor,
                limit = 11,
            ),
        ).thenReturn(listOf(row(id = 1, rating = 4, createdAt = "2026-09-01T00:00:00Z")))

        val result =
            service.getReviews(
                CourseReviewsQuery(
                    courseId = COURSE_ID,
                    sort = CourseReviewSortKey.RATING,
                    descending = false,
                    cursor = CourseReviewCursorCodec.encode(cursor),
                ),
            )

        // 인자가 하나라도 어긋나면 스텁 대신 Mockito 기본값(빈 리스트)이 돌아와 실패한다 — 행이 보이면 전달이 검증된 것.
        assertEquals(listOf(1L), result.reviews.map { it.id })
    }

    private fun row(
        id: Long,
        rating: Int,
        createdAt: String,
        content: String? = null,
    ) = CourseReviewRow(
        id = id,
        userId = USER_ID,
        rating = rating,
        content = content,
        createdAt = Instant.parse(createdAt),
    )

    private companion object {
        const val COURSE_ID = 701L
        const val USER_ID = 1L
    }
}
