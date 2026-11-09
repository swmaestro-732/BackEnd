package com.example.backend.mobile.course.adapter.inbound.web.admin

import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 어드민 미리보기: 앱 코스 상세와 같은 응답 모양이고 아무것도 저장하지 않는다(SCRUM-752). */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/admin-course-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminCoursePreviewControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val adminTokenPort: AdminTokenPort,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        private val body =
            """
            {
              "title": "미리보기 코스",
              "description": "설명",
              "thumbnailUrl": "https://img/preview-cover.jpg",
              "companions": ["DATE"],
              "tags": ["성수", " 성수 ", ""],
              "places": [
                {"placeId": 2, "orderNo": 1, "caption": "둘째", "imageUrls": ["https://img/b.jpg"]},
                {"placeId": 1, "orderNo": 0, "caption": "첫째", "imageUrls": ["https://img/a1.jpg", "https://img/a2.jpg"], "walkingMinutes": 6}
              ]
            }
            """.trimIndent()

        @Test
        fun `초안을 앱 코스 상세 응답 모양으로 계산하고 저장하지 않는다`() {
            val before = tableCounts()

            mockMvc
                .perform(
                    post("/admin/v1/course-previews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${adminToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.title").value("미리보기 코스"))
                .andExpect(jsonPath("$.data.course.coverImageUrl").value("https://img/preview-cover.jpg"))
                // 실제 저장과 같은 규칙: 고른 동행 테마 + 장소(카페 2곳) 기반 테마, enum 선언 순.
                .andExpect(jsonPath("$.data.course.themes", contains("DATE", "CAFETOUR")))
                .andExpect(jsonPath("$.data.course.tags", contains("성수")))
                .andExpect(jsonPath("$.data.course.stats.placeCount").value(2))
                .andExpect(jsonPath("$.data.course.stats.walkingMinutes").value(6))
                .andExpect(jsonPath("$.data.course.stats.tracingCount").value(0))
                .andExpect(jsonPath("$.data.course.author.id").value(2))
                .andExpect(jsonPath("$.data.course.author.nickname").value("큐레이터"))
                .andExpect(jsonPath("$.data.course.places[0].placeId").value(1))
                .andExpect(jsonPath("$.data.course.places[0].name").value("카페A"))
                .andExpect(jsonPath("$.data.course.places[0].categories", contains("CAFE")))
                .andExpect(jsonPath("$.data.course.places[0].location.latitude").value(37.54))
                .andExpect(jsonPath("$.data.course.places[0].images[1].imageUrl").value("https://img/a2.jpg"))
                .andExpect(jsonPath("$.data.course.places[1].walkingMinutesToNext").value(nullValue()))
                .andExpect(jsonPath("$.data.course.viewer.hasSaved").value(false))
                .andExpect(jsonPath("$.data.reviewSummary.totalCount").value(0))
                .andExpect(jsonPath("$.data.reviewSummary.previews").isEmpty)

            assertEquals(before, tableCounts())
        }

        @Test
        fun `앱 access token 은 403 으로 거부한다`() {
            mockMvc
                .perform(
                    post("/admin/v1/course-previews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isForbidden)
        }

        private fun adminToken() = adminTokenPort.issue("admin@example.com", "admin-google-sub").token

        private fun tableCounts(): List<Int?> =
            listOf("courses", "course_places", "course_place_images", "tags", "admin_actions").map {
                jdbcTemplate.queryForObject("SELECT count(*) FROM $it", Int::class.java)
            }
    }
