package com.example.backend.course.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [CourseReview] 도메인 단위 테스트(Spring 컨텍스트 없음).
 * 작성 팩토리의 불변식만 본다 — 별점 범위·사진 상한·한마디 정규화·태그 중복 제거.
 * 웹 DTO 의 Bean Validation 과 상한이 겹치지만, 다른 진입점에서도 규칙이 유지되는지는 여기서 보장한다.
 */
class CourseReviewTest {
    @Test
    fun `작성 직후에는 id·createdAt 이 비어 있고 상태는 PUBLISHED 다`() {
        val review = review(rating = 4)

        assertNull(review.id)
        assertNull(review.createdAt)
        assertEquals(CourseReviewStatus.PUBLISHED, review.status)
        assertEquals(COURSE_ID, review.courseId)
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
        assertEquals("동선이 편했어요", review(content = "  동선이 편했어요  ").content)
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
    fun `태그는 5개까지 고를 수 있다`() {
        val tags = CourseReviewTag.entries.take(5).toSet()

        assertEquals(5, review(tags = tags).tags.size)

        assertThrows<IllegalArgumentException> { review(tags = CourseReviewTag.entries.take(6).toSet()) }
    }

    private fun review(
        rating: Int = 5,
        content: String? = null,
        photoUrls: List<String> = emptyList(),
        tags: Set<CourseReviewTag> = emptySet(),
    ) = CourseReview.create(
        courseId = COURSE_ID,
        userId = USER_ID,
        rating = rating,
        content = content,
        photoUrls = photoUrls,
        tags = tags,
    )

    private companion object {
        const val COURSE_ID = 701L
        const val USER_ID = 1L
    }
}
