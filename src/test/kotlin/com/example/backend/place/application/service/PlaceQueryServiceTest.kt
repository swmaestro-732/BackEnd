package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.place.application.port.inbound.dto.PlaceDetailResult
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

class PlaceQueryServiceTest {
    private val now = Instant.parse("2026-07-11T12:00:00Z")

    private val fakePlacePort =
        object : PlaceQueryPort {
            var place: PlaceQueryPort.PlaceRecord? = null
            var hours: List<PlaceQueryPort.BusinessHourRecord> = emptyList()
            var reviews: List<PlaceQueryPort.ReviewRecord> = emptyList()
            var reviewCount: Int = 0
            var avgRating: Double? = null

            override fun findPlace(placeId: Long) = place

            override fun findBusinessHours(placeId: Long) = hours

            override fun findRecentReviews(
                placeId: Long,
                limit: Int,
            ) = reviews.take(limit)

            override fun countReviews(placeId: Long) = reviewCount

            override fun averageRating(placeId: Long) = avgRating
        }

    private val fakeUserSummaries =
        object : UserSummaryUseCase {
            var summaries: List<UserSummaryUseCase.UserSummary> = emptyList()

            override fun findSummaries(ids: Collection<Long>) = summaries.filter { it.id in ids }
        }

    private val service = PlaceQueryService(fakePlacePort, fakeUserSummaries, Clock.fixed(now, ZoneOffset.UTC))

    private fun place(businessStatus: PlaceBusinessStatus = PlaceBusinessStatus.OPEN) =
        PlaceQueryPort.PlaceRecord(
            id = 1,
            name = "어니언 성수",
            category = "카페",
            imageUrl = "https://cdn.example.com/places/1/1.jpg",
            address = "서울 성동구 아차산로 100",
            latitude = 37.5446,
            longitude = 127.0559,
            businessStatus = businessStatus,
        )

    private fun review(
        id: Long,
        userId: Long,
        rating: Int = 5,
        createdAt: Instant = now.minusSeconds(3 * 24 * 3600),
    ) = PlaceQueryPort.ReviewRecord(
        id = id,
        userId = userId,
        rating = rating,
        content = "리뷰 $id",
        createdAt = createdAt,
        photoUrls = emptyList(),
    )

    @Test
    fun `장소가 없으면 NOT_FOUND 비즈니스 예외를 던진다`() {
        fakePlacePort.place = null

        val e = assertThrows<BusinessException> { service.getDetail(999) }

        assertEquals(ErrorCode.NOT_FOUND, e.errorCode)
    }

    @Test
    fun `장소·리뷰·작성자를 병합해 상세 결과를 만든다`() {
        fakePlacePort.place = place()
        fakePlacePort.reviews = listOf(review(id = 1, userId = 10))
        fakePlacePort.reviewCount = 128
        fakePlacePort.avgRating = 4.8
        fakeUserSummaries.summaries =
            listOf(
                UserSummaryUseCase.UserSummary(
                    id = 10,
                    nickname = "현우님",
                    profileImageUrl = "https://cdn.example.com/users/10.jpg",
                ),
            )

        val result = service.getDetail(1)

        assertEquals("어니언 성수", result.name)
        assertEquals(listOf("카페"), result.categories)
        assertEquals(listOf("https://cdn.example.com/places/1/1.jpg"), result.imageUrls)
        assertEquals(37.5446, result.latitude)
        assertEquals(128, result.reviewSummary.totalCount)
        val r = result.reviewSummary.reviews.single()
        assertEquals(10, r.authorId)
        assertEquals("현우님", r.authorNickname)
        assertEquals("3일 전", r.relativeTime)
        assertFalse(result.viewerHasSaved) // 인증 도입 전까지 false 고정
    }

    @Test
    fun `리뷰 미리보기는 2개까지만 담는다`() {
        fakePlacePort.place = place()
        fakePlacePort.reviews = listOf(review(1, 10), review(2, 11), review(3, 12))
        fakePlacePort.reviewCount = 3
        fakePlacePort.avgRating = 5.0

        val result = service.getDetail(1)

        assertEquals(2, result.reviewSummary.reviews.size)
    }

    @Test
    fun `평균 평점은 소수점 1자리로 반올림한다`() {
        fakePlacePort.place = place()
        fakePlacePort.avgRating = 4.7856

        val result = service.getDetail(1)

        assertEquals(4.8, result.reviewSummary.averageRating)
    }

    @Test
    fun `영업 상태가 OPEN이 아니면 CLOSED로 내려간다`() {
        fakePlacePort.place = place(businessStatus = PlaceBusinessStatus.UNKNOWN)

        val result = service.getDetail(1)

        assertEquals(PlaceDetailResult.OpenStatus.CLOSED, result.openStatus)
    }

    @Test
    fun `영업시간이 요일별로 동일하면 매일 형식으로 표기한다`() {
        fakePlacePort.place = place()
        fakePlacePort.hours =
            (0..6).map {
                PlaceQueryPort.BusinessHourRecord(it, LocalTime.of(11, 0), LocalTime.of(21, 0))
            }

        val result = service.getDetail(1)

        assertEquals("매일 11:00 – 21:00", result.openingHoursText)
    }

    @Test
    fun `영업시간이 7일 전체가 아니면 매일로 표기하지 않는다`() {
        fakePlacePort.place = place()
        fakePlacePort.hours =
            (0..4).map {
                PlaceQueryPort.BusinessHourRecord(it, LocalTime.of(11, 0), LocalTime.of(21, 0))
            }

        val result = service.getDetail(1)

        assertNull(result.openingHoursText)
    }

    @Test
    fun `영업시간이 요일별로 다르면 표기하지 않는다`() {
        fakePlacePort.place = place()
        fakePlacePort.hours =
            listOf(
                PlaceQueryPort.BusinessHourRecord(0, LocalTime.of(11, 0), LocalTime.of(21, 0)),
                PlaceQueryPort.BusinessHourRecord(1, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            )

        val result = service.getDetail(1)

        assertNull(result.openingHoursText)
    }

    @Test
    fun `작성자를 찾을 수 없으면 탈퇴한 사용자로 표기한다`() {
        fakePlacePort.place = place()
        fakePlacePort.reviews = listOf(review(id = 1, userId = 99))
        fakePlacePort.reviewCount = 1
        fakePlacePort.avgRating = 5.0
        fakeUserSummaries.summaries = emptyList()

        val result = service.getDetail(1)

        val r = result.reviewSummary.reviews.single()
        assertEquals("탈퇴한 사용자", r.authorNickname)
        assertNull(r.authorProfileImageUrl)
    }

    @Test
    fun `상대 시간을 한국어로 표기한다`() {
        fakePlacePort.place = place()
        fakePlacePort.reviews =
            listOf(
                review(id = 1, userId = 10, createdAt = now.minusSeconds(30)),
                review(id = 2, userId = 10, createdAt = now.minusSeconds(2 * 3600)),
            )
        fakePlacePort.reviewCount = 2
        fakePlacePort.avgRating = 5.0

        val result = service.getDetail(1)

        assertEquals("방금 전", result.reviewSummary.reviews[0].relativeTime)
        assertEquals("2시간 전", result.reviewSummary.reviews[1].relativeTime)
    }
}
