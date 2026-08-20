package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.persistence.postgis.GeoPoint
import com.example.backend.common.response.ErrorCode
import com.example.backend.place.application.port.inbound.dto.CreatePlaceReviewCommand
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceReviewPersistencePort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceReview
import com.example.backend.place.domain.model.PlaceReviewStatus
import com.example.backend.place.domain.model.PlaceReviewTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Clock

/**
 * [PlaceReviewService] 단위 테스트 — 포트를 페이크로 대체해 서비스 규칙만 검증한다
 * ([com.example.backend.user.application.service.SavedPlaceServiceTest] 와 같은 형식).
 * 검증 대상: 장소 존재 검증(없으면 404), 태그 코드 → 도메인 enum 변환(모르는 코드는 400),
 * 도메인 조립 후 영속 포트 위임.
 */
class PlaceReviewServiceTest {
    private val fakeQueryPort =
        object : PlaceQueryPort {
            /** 살아있는 장소 id 집합. 없는 키는 삭제/부재 장소로 취급한다. */
            var existingPlaceIds: Set<Long> = setOf(PLACE_ID)

            override fun findPlacesById(placeIds: List<Long>): List<Place> =
                placeIds.filter { it in existingPlaceIds }.map { place() }

            override fun searchByName(
                query: String,
                cursor: String?,
                limit: Int,
            ): List<Place> = emptyList()

            override fun countByName(query: String): Long = 0
        }

    private val fakePersistencePort =
        object : PlaceReviewPersistencePort {
            var saved: PlaceReview? = null

            override fun save(review: PlaceReview): PlaceReview {
                saved = review
                return PlaceReview.reconstitute(
                    id = SAVED_REVIEW_ID,
                    placeId = review.placeId,
                    userId = review.userId,
                    status = review.status,
                    rating = review.rating,
                    content = review.content,
                    photoUrls = review.photoUrls,
                    tags = review.tags,
                    createdAt = Clock.System.now(),
                )
            }
        }

    private val service = PlaceReviewService(fakeQueryPort, fakePersistencePort)

    @Test
    fun `리뷰를 저장하고 생성된 id 를 담은 도메인을 돌려준다`() {
        val created =
            service.create(
                command(
                    rating = 5,
                    content = "  통창 뷰가 좋아요  ",
                    photoUrls = listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"),
                    tagCodes = listOf("coffee", "view"),
                ),
            )

        assertEquals(SAVED_REVIEW_ID, created.id)

        val saved = requireNotNull(fakePersistencePort.saved)
        assertEquals(PLACE_ID, saved.placeId)
        assertEquals(USER_ID, saved.userId)
        assertEquals(5, saved.rating)
        assertEquals(PlaceReviewStatus.PUBLISHED, saved.status)
        // 한마디 트림·사진 순서는 도메인 팩토리가 맡는다 — 서비스가 그대로 태우는지만 본다.
        assertEquals("통창 뷰가 좋아요", saved.content)
        assertEquals(listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"), saved.photoUrls)
        assertEquals(listOf(PlaceReviewTag.COFFEE, PlaceReviewTag.VIEW), saved.tags)
    }

    @Test
    fun `별점만 남겨도 저장된다`() {
        service.create(command(rating = 3))

        val saved = requireNotNull(fakePersistencePort.saved)
        assertNull(saved.content)
        assertEquals(emptyList<String>(), saved.photoUrls)
        assertEquals(emptyList<PlaceReviewTag>(), saved.tags)
    }

    @Test
    fun `없는 장소에는 리뷰를 쓸 수 없다`() {
        fakeQueryPort.existingPlaceIds = emptySet()

        val exception = assertThrows<BusinessException> { service.create(command()) }

        assertEquals(ErrorCode.PLACE_NOT_FOUND, exception.errorCode)
        assertNull(fakePersistencePort.saved) // 저장까지 가지 않는다
    }

    @Test
    fun `모르는 태그 코드는 400 이고 저장하지 않는다`() {
        val exception =
            assertThrows<BusinessException> { service.create(command(tagCodes = listOf("coffee", "nosuchtag"))) }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
        assertNull(fakePersistencePort.saved)
    }

    @Test
    fun `태그 코드는 대소문자·앞뒤 공백을 가리지 않는다`() {
        service.create(command(tagCodes = listOf(" COFFEE ", "view")))

        assertEquals(listOf(PlaceReviewTag.COFFEE, PlaceReviewTag.VIEW), requireNotNull(fakePersistencePort.saved).tags)
    }

    @Test
    fun `같은 장소에 여러 번 쓸 수 있다`() {
        // 재방문마다 남길 수 있어야 해 중복 작성을 막지 않는다(스키마에도 유니크 제약이 없다).
        service.create(command(rating = 5))
        service.create(command(rating = 2))

        assertEquals(2, requireNotNull(fakePersistencePort.saved).rating)
    }

    private fun command(
        placeId: Long = PLACE_ID,
        rating: Int = 4,
        content: String? = null,
        photoUrls: List<String> = emptyList(),
        tagCodes: List<String> = emptyList(),
    ) = CreatePlaceReviewCommand(
        placeId = placeId,
        userId = USER_ID,
        rating = rating,
        content = content,
        photoUrls = photoUrls,
        tagCodes = tagCodes,
    )

    private fun place() =
        Place.create(
            name = "어니언 성수",
            description = null,
            category = PlaceCategory.CAFE,
            location = GeoPoint(latitude = 37.5446, longitude = 127.0559),
            address = "서울 성동구 아차산로 100",
            imageUrl = null,
        )

    private companion object {
        const val PLACE_ID = 601L
        const val USER_ID = 1L
        const val SAVED_REVIEW_ID = 100L
    }
}
