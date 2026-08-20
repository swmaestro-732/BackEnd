package com.example.backend.place.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [PlaceReview] 도메인 단위 테스트(Spring 컨텍스트 없음).
 * 작성 팩토리의 불변식만 본다 — 별점 범위·사진 상한·한마디 정규화·태그 중복 제거.
 * 웹 DTO 의 Bean Validation 과 상한이 겹치지만, 다른 진입점에서도 규칙이 유지되는지는 여기서 보장한다.
 */
class PlaceReviewTest {
    @Test
    fun `작성 직후에는 id·createdAt 이 비어 있고 상태는 PUBLISHED 다`() {
        val review = review(rating = 4)

        assertNull(review.id)
        assertNull(review.createdAt)
        assertEquals(PlaceReviewStatus.PUBLISHED, review.status)
        assertEquals(PLACE_ID, review.placeId)
        assertEquals(USER_ID, review.userId)
    }

    @Test
    fun `별점은 1~5 만 허용한다`() {
        assertEquals(1, review(rating = 1).rating)
        assertEquals(5, review(rating = 5).rating)

        assertThrows<IllegalArgumentException> { review(rating = 0) }
        assertThrows<IllegalArgumentException> { review(rating = 6) }
    }

    @Test
    fun `한마디는 앞뒤 공백을 잘라 저장한다`() {
        assertEquals("통창 뷰가 좋아요", review(content = "  통창 뷰가 좋아요  ").content)
    }

    @Test
    fun `한마디가 없거나 공백뿐이면 null 로 정규화한다`() {
        assertNull(review(content = null).content)
        assertNull(review(content = "   ").content)
    }

    @Test
    fun `한마디는 1000자까지 쓸 수 있다`() {
        assertEquals(1000, review(content = "가".repeat(1000)).content?.length)

        assertThrows<IllegalArgumentException> { review(content = "가".repeat(1001)) }
    }

    @Test
    fun `한마디 길이는 트림한 값 기준이다`() {
        // 원본은 1002자지만 앞뒤 공백을 빼면 1000자다 — 화면 글자수 카운터(트림 기준)와 어긋나지 않게 한다.
        val content = " " + "가".repeat(1000) + " "

        assertEquals(1000, review(content = content).content?.length)
    }

    @Test
    fun `사진은 6장까지 올릴 수 있고 순서를 그대로 유지한다`() {
        val photoUrls = (1..6).map { "https://cdn.example.com/$it.jpg" }

        assertEquals(photoUrls, review(photoUrls = photoUrls).photoUrls)

        assertThrows<IllegalArgumentException> { review(photoUrls = photoUrls + "https://cdn.example.com/7.jpg") }
    }

    @Test
    fun `빈 사진 URL 은 받지 않는다`() {
        assertThrows<IllegalArgumentException> { review(photoUrls = listOf("  ")) }
    }

    @Test
    fun `같은 태그가 여러 번 오면 하나로 접는다`() {
        // 링크 테이블 PK 가 (리뷰, 태그)라 중복이 그대로 가면 저장에서 터진다.
        val review =
            review(
                tags = listOf(PlaceReviewTag.COFFEE, PlaceReviewTag.VIEW, PlaceReviewTag.COFFEE),
            )

        assertEquals(listOf(PlaceReviewTag.COFFEE, PlaceReviewTag.VIEW), review.tags)
    }

    @Test
    fun `태그는 10개까지 고를 수 있다`() {
        val tags = PlaceReviewTag.entries.take(10)

        assertEquals(10, review(tags = tags).tags.size)

        assertThrows<IllegalArgumentException> { review(tags = PlaceReviewTag.entries.take(11)) }
    }

    @Test
    fun `태그 상한은 중복을 접은 뒤 개수로 센다`() {
        val tags = PlaceReviewTag.entries.take(10) + PlaceReviewTag.entries.take(5)

        assertEquals(10, review(tags = tags).tags.size)
    }

    private fun review(
        rating: Int = 5,
        content: String? = null,
        photoUrls: List<String> = emptyList(),
        tags: List<PlaceReviewTag> = emptyList(),
    ) = PlaceReview.create(
        placeId = PLACE_ID,
        userId = USER_ID,
        rating = rating,
        content = content,
        photoUrls = photoUrls,
        tags = tags,
    )

    private companion object {
        const val PLACE_ID = 601L
        const val USER_ID = 1L
    }
}
