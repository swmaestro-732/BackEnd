package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseSearchResult
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [CourseSearchResponse.from] 단위 테스트 — 순수 매핑, DB·Spring 컨텍스트 불필요.
 * CourseSearchResult → CourseSearchResponse 필드 1:1 변환과 null 전파를 검증한다.
 */
class CourseSearchResponseTest {
    private val now = Instant.parse("2024-06-01T00:00:00Z")

    private fun summary(id: Long) =
        CourseSummary(
            id = id,
            authorId = 10L,
            title = "코스 $id",
            coverImageUrl = "https://img/$id.jpg",
            theme = "CAFETOUR",
            area = "서울",
            likesCnt = id.toInt() * 10,
            savesCnt = id.toInt() * 5,
            createdAt = now,
        )

    @Test
    fun `from 은 courses·nextCursor·hasNext 를 그대로 변환한다`() {
        val result =
            CourseSearchResult(
                courses = listOf(summary(1L), summary(2L)),
                nextCursor = "cursor-abc",
                hasNext = true,
            )

        val response = CourseSearchResponse.from(result)

        assertThat(response.nextCursor).isEqualTo("cursor-abc")
        assertThat(response.hasNext).isTrue()
        assertThat(response.courses).hasSize(2)
    }

    @Test
    fun `Item 의 모든 필드가 CourseSummary 와 일치한다`() {
        val result =
            CourseSearchResult(
                courses = listOf(summary(42L)),
                nextCursor = null,
                hasNext = false,
            )

        val item = CourseSearchResponse.from(result).courses[0]

        assertThat(item.id).isEqualTo(42L)
        assertThat(item.authorId).isEqualTo(10L)
        assertThat(item.title).isEqualTo("코스 42")
        assertThat(item.coverImageUrl).isEqualTo("https://img/42.jpg")
        assertThat(item.theme).isEqualTo("CAFETOUR")
        assertThat(item.area).isEqualTo("서울")
        assertThat(item.likesCnt).isEqualTo(420)
        assertThat(item.savesCnt).isEqualTo(210)
        assertThat(item.createdAt).isEqualTo(now)
    }

    @Test
    fun `hasNext=false, nextCursor=null 이면 그대로 전달한다`() {
        val response =
            CourseSearchResponse.from(
                CourseSearchResult(courses = listOf(summary(1L)), nextCursor = null, hasNext = false),
            )

        assertThat(response.nextCursor).isNull()
        assertThat(response.hasNext).isFalse()
    }

    @Test
    fun `빈 코스 목록도 변환한다`() {
        val response =
            CourseSearchResponse.from(
                CourseSearchResult(courses = emptyList(), nextCursor = null, hasNext = false),
            )

        assertThat(response.courses).isEmpty()
    }

    @Test
    fun `nullable 필드(coverImageUrl, theme, area) 는 null 도 그대로 전달한다`() {
        val nullableSummary =
            CourseSummary(
                id = 99L,
                authorId = 1L,
                title = "null 필드 코스",
                coverImageUrl = null,
                theme = null,
                area = null,
                likesCnt = 0,
                savesCnt = 0,
                createdAt = now,
            )
        val item =
            CourseSearchResponse
                .from(CourseSearchResult(courses = listOf(nullableSummary), nextCursor = null, hasNext = false))
                .courses[0]

        assertThat(item.coverImageUrl).isNull()
        assertThat(item.theme).isNull()
        assertThat(item.area).isNull()
    }
}
