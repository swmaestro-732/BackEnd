package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.course.application.port.inbound.CourseRecommendUseCase
import com.example.backend.course.application.port.inbound.dto.CourseRecommendPage
import com.example.backend.course.application.port.inbound.dto.CourseRecommendQuery
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import com.example.backend.mobile.aichat.application.port.outbound.AiChatCourseCriteria
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatCourse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

/** [AiChatCourseAdapter] — 채팅 조건을 course 추천 질의로 옮기고 요약을 카드로 줄이는 매핑을 검증한다. */
class AiChatCourseAdapterTest {
    private val useCase = mock(CourseRecommendUseCase::class.java)
    private val adapter = AiChatCourseAdapter(useCase)

    private fun <T> anyArg(): T = ArgumentMatchers.any()

    private fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()

    private fun summary(
        id: Long,
        placeCount: Int?,
        walkingMinutes: Int?,
    ) = CourseSummary(
        id = id,
        authorId = 3L,
        title = "코스 $id",
        coverImageUrl = "https://img/$id.jpg",
        themes = listOf("DATE", "CAFETOUR"),
        area = "성수",
        likesCnt = 4,
        savesCnt = 9,
        createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        placeCount = placeCount,
        walkingMinutes = walkingMinutes,
    )

    @Test
    fun `조건을 추천 질의로 옮기고 결과를 카드로 줄인다`() {
        `when`(useCase.recommend(anyArg<CourseRecommendQuery>()))
            .thenReturn(
                CourseRecommendPage(
                    listOf(summary(1, 3, 25), summary(2, null, null)),
                    hasNext = true,
                    nextCursor = "c",
                ),
            )

        val courses =
            adapter.recommend(
                AiChatCourseCriteria(
                    regionPrefixes = listOf("1120011400"),
                    category = "DATE",
                    likeThemes = listOf("DATE"),
                    keyword = "실내",
                    placeId = 10L,
                    excludeIds = listOf(7L),
                    size = 3,
                ),
            )

        val captor: ArgumentCaptor<CourseRecommendQuery> = ArgumentCaptor.forClass(CourseRecommendQuery::class.java)
        verify(useCase).recommend(capture(captor))
        val query = captor.value
        assertThat(query.likeThemes).containsExactly("DATE")
        assertThat(query.areaCodes).isEmpty()
        assertThat(query.cursor).isNull()
        assertThat(query.size).isEqualTo(3)
        assertThat(query.category).isEqualTo("DATE")
        assertThat(query.region).isNull()
        assertThat(query.regionPrefixes).containsExactly("1120011400")
        assertThat(query.placeId).isEqualTo(10L)
        assertThat(query.excludeIds).containsExactly(7L)
        assertThat(query.keyword).isEqualTo("실내")

        assertThat(courses).containsExactly(
            AiChatCourse(1L, "코스 1", "https://img/1.jpg", listOf("DATE", "CAFETOUR"), "성수", 3, 25, 4, 9),
            AiChatCourse(2L, "코스 2", "https://img/2.jpg", listOf("DATE", "CAFETOUR"), "성수", 0, 0, 4, 9),
        )
    }
}
