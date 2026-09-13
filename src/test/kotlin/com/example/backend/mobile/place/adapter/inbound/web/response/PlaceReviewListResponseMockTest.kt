package com.example.backend.mobile.place.adapter.inbound.web.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlaceReviewListResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = PlaceReviewListResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() 평균 평점은 양수다`() {
        val response = PlaceReviewListResponse.mock()

        assertThat(response.averageRating).isGreaterThan(0.0)
    }

    @Test
    fun `mock() totalCount 는 reviews 개수와 일치한다`() {
        val response = PlaceReviewListResponse.mock()

        assertThat(response.totalCount).isEqualTo(response.reviews.size)
    }

    @Test
    fun `mock() ratingDistribution 은 5개 항목(5~1점)이다`() {
        val response = PlaceReviewListResponse.mock()

        assertThat(response.ratingDistribution).hasSize(5)
        assertThat(response.ratingDistribution.map { it.rating }).containsExactly(5, 4, 3, 2, 1)
    }

    @Test
    fun `mock() ratingDistribution 각 별점 개수가 reviews 와 일치하고 합계가 totalCount 와 같다`() {
        val response = PlaceReviewListResponse.mock()

        // ratings: [5,4,5,3,4,5] → 5→3, 4→2, 3→1, 2→0, 1→0
        val countByRating = response.ratingDistribution.associate { it.rating to it.count }
        assertThat(countByRating[5]).isEqualTo(response.reviews.count { it.rating == 5 })
        assertThat(countByRating[4]).isEqualTo(response.reviews.count { it.rating == 4 })
        assertThat(countByRating[3]).isEqualTo(response.reviews.count { it.rating == 3 })
        assertThat(countByRating[2]).isEqualTo(response.reviews.count { it.rating == 2 })
        assertThat(countByRating[1]).isEqualTo(response.reviews.count { it.rating == 1 })
        assertThat(response.ratingDistribution.sumOf { it.count }).isEqualTo(response.totalCount)
    }

    @Test
    fun `mock() photoCount 는 reviews 의 photoUrls 합계와 일치한다`() {
        val response = PlaceReviewListResponse.mock()

        val expected = response.reviews.sumOf { it.photoUrls.size }
        assertThat(response.photoCount).isEqualTo(expected)
    }

    @Test
    fun `mock() hasNext 는 false 이고 nextCursor 는 null 이다`() {
        val response = PlaceReviewListResponse.mock()

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
    }

    @Test
    fun `mock() 각 리뷰는 id·author·content 가 채워져 있다`() {
        val response = PlaceReviewListResponse.mock()

        response.reviews.forEach { review ->
            assertThat(review.id).isPositive
            assertThat(review.author.nickname).isNotBlank
            assertThat(review.content).isNotBlank
            assertThat(review.rating).isBetween(1, 5)
        }
    }
}
