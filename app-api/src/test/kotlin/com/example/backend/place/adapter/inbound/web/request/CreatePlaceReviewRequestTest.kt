package com.example.backend.place.adapter.inbound.web.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreatePlaceReviewRequestTest {
    @Test
    fun `rating 만 넘기면 content·photoUrls·tagCodes 는 기본값이다`() {
        val request = CreatePlaceReviewRequest(rating = 5)

        assertThat(request.rating).isEqualTo(5)
        assertThat(request.content).isNull()
        assertThat(request.photoUrls).isEmpty()
        assertThat(request.tagCodes).isEmpty()
    }

    @Test
    fun `전체 필드를 채운 요청을 생성할 수 있다`() {
        val request =
            CreatePlaceReviewRequest(
                rating = 4,
                content = "좋았어요",
                photoUrls = listOf("https://cdn.example.com/photo.jpg"),
                tagCodes = listOf("coffee", "view"),
            )

        assertThat(request.rating).isEqualTo(4)
        assertThat(request.content).isEqualTo("좋았어요")
        assertThat(request.photoUrls).hasSize(1)
        assertThat(request.tagCodes).containsExactly("coffee", "view")
    }
}
