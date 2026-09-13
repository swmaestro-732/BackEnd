package com.example.backend.course.adapter.inbound.web.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateCourseReviewRequestTest {
    @Test
    fun `rating 만 넘기면 content·photoUrls·tagCodes 는 기본값이다`() {
        val request = CreateCourseReviewRequest(rating = 5)

        assertThat(request.rating).isEqualTo(5)
        assertThat(request.content).isNull()
        assertThat(request.photoUrls).isEmpty()
        assertThat(request.tagCodes).isEmpty()
    }

    @Test
    fun `전체 필드를 채운 요청을 생성할 수 있다`() {
        val request =
            CreateCourseReviewRequest(
                rating = 3,
                content = "동선이 좋았어요",
                photoUrls = listOf("https://cdn.example.com/photo1.jpg", "https://cdn.example.com/photo2.jpg"),
                tagCodes = listOf("packed", "walkable"),
            )

        assertThat(request.rating).isEqualTo(3)
        assertThat(request.content).isEqualTo("동선이 좋았어요")
        assertThat(request.photoUrls).containsExactly(
            "https://cdn.example.com/photo1.jpg",
            "https://cdn.example.com/photo2.jpg",
        )
        assertThat(request.tagCodes).containsExactly("packed", "walkable")
    }
}
