package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.dto.PlaceReviewSortKey
import com.example.backend.place.application.port.inbound.dto.PlaceReviewsQuery
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceReviewCursor
import com.example.backend.place.application.port.outbound.PlaceReviewQueryPort
import com.example.backend.place.application.port.outbound.PlaceReviewRow
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceReviewTag
import com.example.backend.place.domain.model.PlaceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [PlaceReviewQueryService] 단위 테스트 — 포트를 페이크로 대체해 페이지 조립 규칙만 검증한다
 * ([PlaceReviewServiceTest] 와 같은 형식).
 * 검증 대상: size+1 조회의 hasNext/nextCursor 판정, 사진·태그 배치 병합, 분포 5~1점 고정 채움,
 * 장소 카운터(rating_sum/rating_cnt) 기반 평균·총개수, 없는 장소의 빈 목록(집계 0) 규칙.
 * (커서 인코딩 형식·비정상 커서 방어는 [PlaceReviewCursorCodecTest] 가 커버한다.)
 */
class PlaceReviewQueryServiceTest {
    private val fakeReviewQueryPort =
        object : PlaceReviewQueryPort {
            var rows: List<PlaceReviewRow> = emptyList()
            var photoUrls: Map<Long, List<String>> = emptyMap()
            var tags: Map<Long, List<PlaceReviewTag>> = emptyMap()
            var ratingCounts: Map<Int, Long> = emptyMap()
            var photoCount: Long = 0
            var receivedCursor: PlaceReviewCursor? = null
            var receivedLimit: Int = 0

            override fun findReviewsByPlace(
                placeId: Long,
                sort: PlaceReviewSortKey,
                descending: Boolean,
                cursor: PlaceReviewCursor?,
                limit: Int,
            ): List<PlaceReviewRow> {
                receivedCursor = cursor
                receivedLimit = limit
                return rows
            }

            override fun findPhotoUrls(reviewIds: List<Long>): Map<Long, List<String>> = photoUrls

            override fun findTags(reviewIds: List<Long>): Map<Long, List<PlaceReviewTag>> = tags

            override fun countReviewsByRating(placeId: Long): Map<Int, Long> = ratingCounts

            override fun countPhotosByPlace(placeId: Long): Long = photoCount
        }

    private val fakePlaceQueryPort =
        object : PlaceQueryPort {
            /** 살아있는 장소와 그 카운터. null 이면 없는(삭제된) 장소다. */
            var place: Place? = null

            override fun findPlaceById(placeId: Long): Place? = place

            override fun findPlacesById(placeIds: List<Long>): List<Place> = listOfNotNull(place)

            override fun searchByName(
                query: String,
                cursor: String?,
                limit: Int,
            ): List<Place> = emptyList()

            override fun countByName(query: String): Long = 0
        }

    private val service = PlaceReviewQueryService(fakeReviewQueryPort, fakePlaceQueryPort)

    @Test
    fun `한 페이지를 집계·자식과 함께 조립한다`() {
        // size+1(3)개가 돌아오면 마지막 행은 잘려 나가고 hasNext 판정에만 쓰인다.
        fakeReviewQueryPort.rows =
            listOf(
                row(id = 3, rating = 5, createdAt = "2026-09-03T00:00:00Z"),
                row(id = 2, rating = 4, createdAt = "2026-09-02T00:00:00Z", content = "통창 뷰가 좋아요"),
                row(id = 1, rating = 3, createdAt = "2026-09-01T00:00:00Z"),
            )
        fakeReviewQueryPort.photoUrls = mapOf(3L to listOf("https://cdn.example.com/3-1.jpg"))
        fakeReviewQueryPort.tags = mapOf(3L to listOf(PlaceReviewTag.COFFEE))
        fakeReviewQueryPort.ratingCounts = mapOf(5 to 1L, 4 to 1L, 3 to 1L)
        fakeReviewQueryPort.photoCount = 4
        fakePlaceQueryPort.place = place(ratingSum = 12, ratingCnt = 3)

        val result = service.getReviews(PlaceReviewsQuery(placeId = PLACE_ID, size = 2))

        assertEquals(3, fakeReviewQueryPort.receivedLimit) // size + 1
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
        assertEquals(listOf("coffee"), result.reviews[0].tags.map { it.code })
        assertEquals(emptyList<String>(), result.reviews[1].photoUrls)
        assertEquals("통창 뷰가 좋아요", result.reviews[1].content)
        // nextCursor 는 페이지 마지막 행(id=2)의 키다.
        assertEquals(
            PlaceReviewCursor(rating = 4, createdAt = Instant.parse("2026-09-02T00:00:00Z"), id = 2),
            PlaceReviewCursorCodec.decode(result.nextCursor),
        )
    }

    @Test
    fun `마지막 페이지면 hasNext 없이 nextCursor 도 비운다`() {
        fakeReviewQueryPort.rows = listOf(row(id = 1, rating = 5, createdAt = "2026-09-01T00:00:00Z"))
        fakeReviewQueryPort.ratingCounts = mapOf(5 to 1L)
        fakePlaceQueryPort.place = place(ratingSum = 5, ratingCnt = 1)

        val result = service.getReviews(PlaceReviewsQuery(placeId = PLACE_ID))

        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
        assertEquals(listOf(1L), result.reviews.map { it.id })
    }

    @Test
    fun `없는 장소는 404 가 아니라 빈 목록과 집계 0 이다`() {
        // findPlaceById 가 null(삭제·부재 장소)이어도 예외 없이 0 으로 채운다 — 존재 판정은 상세 화면 몫.
        val result = service.getReviews(PlaceReviewsQuery(placeId = 999999L))

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
        val cursor = PlaceReviewCursor(rating = 4, createdAt = Instant.parse("2026-09-02T00:00:00.123456Z"), id = 2)

        service.getReviews(PlaceReviewsQuery(placeId = PLACE_ID, cursor = PlaceReviewCursorCodec.encode(cursor)))

        assertEquals(cursor, fakeReviewQueryPort.receivedCursor)
    }

    private fun row(
        id: Long,
        rating: Int,
        createdAt: String,
        content: String? = null,
    ) = PlaceReviewRow(
        id = id,
        userId = USER_ID,
        rating = rating,
        content = content,
        createdAt = Instant.parse(createdAt),
    )

    private fun place(
        ratingSum: Long,
        ratingCnt: Int,
    ) = Place.reconstitute(
        id = PLACE_ID,
        status = PlaceStatus.ACTIVE,
        name = "어니언 성수",
        description = null,
        category = PlaceCategory.CAFE,
        location = Coordinate(latitude = 37.5446, longitude = 127.0559),
        address = "서울 성동구 아차산로 100",
        imageUrl = null,
        businessStatus = PlaceBusinessStatus.UNKNOWN,
        kakaoPlaceId = null,
        createdAt = null,
        updatedAt = null,
        deletedAt = null,
        ratingSum = ratingSum,
        ratingCnt = ratingCnt,
    )

    private companion object {
        const val PLACE_ID = 601L
        const val USER_ID = 1L
    }
}
