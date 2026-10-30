package com.example.backend.course.adapter.inbound.web.v2

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 코스 생성·수정 v2(`/api/v2/courses`) 통합 테스트 — 작성자가 고른 카테고리 여러 개가 그대로 저장되는지 본다.
 * 픽스처는 v1 컨트롤러 테스트와 같은 course-crud-fixture.sql 을 쓴다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-crud-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseV2ControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `고른 카테고리로 코스를 생성하면 장소로 도출하지 않고 중복을 합쳐 저장한다`() {
            val body =
                """
                {
                  "title": "새 발행 코스",
                  "thumbnailUrl": "https://img/new-cover.jpg",
                  "tags": ["데이트"],
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "categories": ["NIGHTVIEW", "DATE", "NIGHTVIEW"],
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"], "walkingMinutes": 5},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                  ]
                }
                """.trimIndent()

            val response =
                mockMvc
                    .perform(
                        post("/api/v2/courses")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isCreated)
                    .andExpect(jsonPath("$.data.courseId").isNumber)
                    .andReturn()

            val courseId = extractCourseId(response.response.contentAsString)

            // 장소가 모두 CAFE 라 v1 이면 CAFETOUR 가 도출되지만, v2 는 고른 값만 enum 선언 순으로 담는다.
            mockMvc
                .perform(get("/api/v1/courses/$courseId"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.themes", contains("DATE", "NIGHTVIEW")))
                .andExpect(jsonPath("$.data.course.theme").value("DATE"))
        }

        @Test
        fun `고른 카테고리로 임시저장 코스를 발행 수정하면 카테고리를 덮어쓴다`() {
            val body =
                """
                {
                  "title": "발행 전환 코스",
                  "thumbnailUrl": "https://img/cover.jpg",
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "categories": ["FOOD", "HEALING"],
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    patch("/api/v2/courses/$DRAFT_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.courseId").value(DRAFT_COURSE_ID))

            mockMvc
                .perform(
                    get("/api/v1/courses/$DRAFT_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.themes", contains("HEALING", "FOOD")))
        }

        @Test
        fun `mock=true 면 생성·수정 모두 저장 없이 고정 응답을 내려준다`() {
            val body =
                """
                {"visibility": "PUBLIC", "isPublished": false, "categories": []}
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v2/courses?mock=true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.courseId").isNumber)

            mockMvc
                .perform(
                    patch("/api/v2/courses/$DRAFT_COURSE_ID?mock=true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.courseId").isNumber)
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private fun extractCourseId(json: String): Long =
            Regex(""""courseId"\s*:\s*(\d+)""")
                .find(json)
                ?.groupValues
                ?.get(1)
                ?.toLong()
                ?: error("courseId 를 찾을 수 없습니다: $json")

        private companion object {
            const val OWNER_ID = 1L
            const val DRAFT_COURSE_ID = 2L
        }
    }
