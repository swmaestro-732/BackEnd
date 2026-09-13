package com.example.backend.mobile.course.adapter.inbound.web.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CourseReviewListResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = CourseReviewListResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() 평균 평점은 양수다`() {
        val response = CourseReviewListResponse.mock()

        assertThat(response.averageRating).isGreaterThan(0.0)
    }

    @Test
    fun `mock() totalCount 는 reviews 개수와 일치한다`() {
        val response = CourseReviewListResponse.mock()

        assertThat(response.totalCount).isEqualTo(response.reviews.size)
    }

    @Test
    fun `mock() ratingDistribution 은 5개 항목(5~1점)이다`() {
        val response = CourseReviewListResponse.mock()

        assertThat(response.ratingDistribution).hasSize(5)
        assertThat(response.ratingDistribution.map { it.rating }).containsExactly(5, 4, 3, 2, 1)
    }

    @Test
    fun `mock() photoCount 는 reviews 의 photoUrls 합계와 일치한다`() {
        val response = CourseReviewListResponse.mock()

        val expected = response.reviews.sumOf { it.photoUrls.size }
        assertThat(response.photoCount).isEqualTo(expected)
    }

    @Test
    fun `mock() hasNext 는 false 이고 nextCursor 는 null 이다`() {
        val response = CourseReviewListResponse.mock()

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
    }

    @Test
    fun `mock() 각 리뷰는 id·author·content·rating 이 채워져 있다`() {
        val response = CourseReviewListResponse.mock()

        response.reviews.forEach { review ->
            assertThat(review.id).isPositive
            assertThat(review.author.nickname).isNotBlank
            assertThat(review.content).isNotBlank
            assertThat(review.rating).isBetween(1, 5)
        }
    }
}
