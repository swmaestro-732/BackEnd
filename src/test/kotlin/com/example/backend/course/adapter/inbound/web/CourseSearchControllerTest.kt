package com.example.backend.course.adapter.inbound.web

import com.example.backend.course.application.port.inbound.CourseSearchUseCase
import com.example.backend.course.application.port.inbound.dto.CourseSearchCommand
import com.example.backend.course.application.port.inbound.dto.CourseSearchResult
import com.example.backend.course.application.port.inbound.dto.CourseSearchSort
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

/**
 * [CourseSearchController] 단위 테스트 — fake [CourseSearchUseCase] 를 주입해
 * Request → Command 조립과 결과 Response 매핑을 검증한다(통합 컨텍스트 불필요).
 * Mockito.any() 의 Kotlin null-intrinsic 충돌을 피하려고 CourseSearchServiceTest 와 동일하게 fake 패턴을 쓴다.
 */
class CourseSearchControllerTest {
    /** 호출된 command 를 기록하고 미리 설정한 결과를 돌려주는 가짜 유스케이스. */
    private class FakeUseCase : CourseSearchUseCase {
        var lastCommand: CourseSearchCommand? = null
        var result: CourseSearchResult = CourseSearchResult(emptyList(), null, false)

        override fun search(command: CourseSearchCommand): CourseSearchResult {
            lastCommand = command
            return result
        }
    }

    private val fake = FakeUseCase()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(CourseSearchController(fake)).build()
    }

    private val now = Instant.parse("2024-06-01T00:00:00Z")

    private fun summary(id: Long) =
        CourseSummary(
            id = id,
            authorId = 10L,
            title = "코스 $id",
            coverImageUrl = "https://img/$id.jpg",
            theme = "CAFETOUR",
            area = "서울",
            likesCnt = 100,
            savesCnt = 50,
            createdAt = now,
        )

    @Test
    fun `keyword·area·category·tags·sort·cursor·size 를 CourseSearchCommand 로 조립해 유스케이스에 전달한다`() {
        fake.result = CourseSearchResult(courses = listOf(summary(1L)), nextCursor = "NEXT", hasNext = true)

        mockMvc
            .perform(
                get("/api/v1/courses/search")
                    .param("keyword", "카페")
                    .param("area", "서울")
                    .param("category", "CAFETOUR")
                    .param("tags", "감성", "데이트")
                    .param("sort", "LATEST")
                    .param("cursor", "prev-cursor")
                    .param("size", "10"),
            ).andExpect(status().isOk)

        val cmd = requireNotNull(fake.lastCommand)
        assertThat(cmd.keyword).isEqualTo("카페")
        assertThat(cmd.area).isEqualTo("서울")
        assertThat(cmd.category).isEqualTo("CAFETOUR")
        assertThat(cmd.tags).containsExactlyInAnyOrder("감성", "데이트")
        assertThat(cmd.sort).isEqualTo(CourseSearchSort.LATEST)
        assertThat(cmd.cursor).isEqualTo("prev-cursor")
        assertThat(cmd.size).isEqualTo(10)
    }

    @Test
    fun `유스케이스 결과가 CourseSearchResponse 로 변환되어 응답에 담긴다`() {
        fake.result = CourseSearchResult(courses = listOf(summary(42L)), nextCursor = "cursor-xyz", hasNext = true)

        mockMvc
            .perform(get("/api/v1/courses/search").param("keyword", "카페"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(2000))
            .andExpect(jsonPath("$.data.courses[0].id").value(42))
            .andExpect(jsonPath("$.data.courses[0].authorId").value(10))
            .andExpect(jsonPath("$.data.courses[0].title").value("코스 42"))
            .andExpect(jsonPath("$.data.courses[0].coverImageUrl").value("https://img/42.jpg"))
            .andExpect(jsonPath("$.data.courses[0].theme").value("CAFETOUR"))
            .andExpect(jsonPath("$.data.courses[0].area").value("서울"))
            .andExpect(jsonPath("$.data.courses[0].likesCnt").value(100))
            .andExpect(jsonPath("$.data.courses[0].savesCnt").value(50))
            .andExpect(jsonPath("$.data.nextCursor").value("cursor-xyz"))
            .andExpect(jsonPath("$.data.hasNext").value(true))
    }

    @Test
    fun `optional 파라미터를 생략하면 기본값(sort=RELEVANCE, size=20, tags 빈 목록)으로 호출한다`() {
        mockMvc.perform(get("/api/v1/courses/search").param("keyword", "맛집")).andExpect(status().isOk)

        val cmd = requireNotNull(fake.lastCommand)
        assertThat(cmd.sort).isEqualTo(CourseSearchSort.RELEVANCE)
        assertThat(cmd.size).isEqualTo(20)
        assertThat(cmd.cursor).isNull()
        assertThat(cmd.area).isNull()
        assertThat(cmd.category).isNull()
        assertThat(cmd.tags).isEmpty()
    }
}
