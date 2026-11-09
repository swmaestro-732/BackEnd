package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 숨김(HIDDEN) 코스 노출 점검(SCRUM-752). 숨긴 코스는 어드민 API 밖에서 소유자를 포함한 모두에게 없는 코스(404 4041)다.
 * 픽스처(admin-course-fixture)의 course 4 가 큐레이터(user 2)의 숨긴 발행 코스다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/admin-course-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class HiddenCourseExposureTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `코스 상세 화면은 404 다`() {
            expectNotFound(get("/service/v1/courses/$HIDDEN_COURSE_ID").asUser(OTHER_ID))
        }

        @Test
        fun `댓글을 달 수 없다`() {
            expectNotFound(
                post("/api/v1/courses/$HIDDEN_COURSE_ID/comments").asUser(OTHER_ID).json("""{"content":"댓글"}"""),
            )
        }

        @Test
        fun `리뷰를 쓸 수 없다`() {
            expectNotFound(post("/api/v1/courses/$HIDDEN_COURSE_ID/reviews").asUser(OTHER_ID).json("""{"rating":5}"""))
        }

        @Test
        fun `계획의 원본 코스로 쓸 수 없다`() {
            expectNotFound(
                post("/api/v1/plans").asUser(OTHER_ID).json("""{"title":"계획","sourceCourseId":$HIDDEN_COURSE_ID}"""),
            )
        }

        @Test
        fun `저장할 수 없다`() {
            expectNotFound(post("/api/v1/saved-courses").asUser(OTHER_ID).json("""{"courseId":$HIDDEN_COURSE_ID}"""))
        }

        @Test
        fun `소유자도 편집할 수 없다`() {
            val body =
                """
                {"title":"편집","thumbnailUrl":"https://img/c.jpg","visibility":"PUBLIC","isPublished":true,
                 "places":[{"placeId":1,"orderNo":0,"imageUrls":["https://img/a.jpg"]},
                           {"placeId":2,"orderNo":1,"imageUrls":["https://img/b.jpg"]}]}
                """.trimIndent()
            expectNotFound(patch("/api/v1/courses/$HIDDEN_COURSE_ID").asUser(OWNER_ID).json(body))
        }

        @Test
        fun `소유자도 삭제할 수 없다`() {
            expectNotFound(delete("/api/v1/courses/$HIDDEN_COURSE_ID").asUser(OWNER_ID))
        }

        @Test
        fun `숨긴 원본에서 복제한 초안을 편집해도 원본 참조는 유지된다`() {
            jdbcTemplate.update(
                "INSERT INTO courses (id, user_id, title, is_published, visibility, duplicated_from_id) " +
                    "VALUES (100, 3, '복제 초안', false, 'PRIVATE', 4)",
            )
            jdbcTemplate.update(
                "INSERT INTO course_places (course_id, place_id, order_no) VALUES (100, 1, 0), (100, 2, 1)",
            )
            val body =
                """
                {"title":"복제 초안 편집","visibility":"PRIVATE","isPublished":false,
                 "places":[{"placeId":1,"orderNo":0},{"placeId":2,"orderNo":1}]}
                """.trimIndent()

            mockMvc.perform(patch("/api/v1/courses/100").asUser(OTHER_ID).json(body)).andExpect(status().isOk)

            assertEquals(
                HIDDEN_COURSE_ID,
                jdbcTemplate.queryForObject("SELECT duplicated_from_id FROM courses WHERE id = 100", Long::class.java),
            )
        }

        @Test
        fun `좋아요와 따라가기를 할 수 없다`() {
            expectNotFound(post("/api/v1/courses/$HIDDEN_COURSE_ID/likes").asUser(OTHER_ID))
            expectNotFound(post("/api/v1/courses/$HIDDEN_COURSE_ID/traces").asUser(OTHER_ID))
        }

        private fun expectNotFound(request: MockHttpServletRequestBuilder) {
            mockMvc
                .perform(request)
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        private fun MockHttpServletRequestBuilder.asUser(userId: Long) =
            header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(userId)}")

        private fun MockHttpServletRequestBuilder.json(body: String) =
            contentType(MediaType.APPLICATION_JSON).content(body)

        private companion object {
            const val HIDDEN_COURSE_ID = 4L
            const val OWNER_ID = 2L
            const val OTHER_ID = 3L
        }
    }
