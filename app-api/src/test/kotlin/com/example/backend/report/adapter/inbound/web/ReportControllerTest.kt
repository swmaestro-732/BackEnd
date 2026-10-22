package com.example.backend.report.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

@AutoConfigureMockMvc
@Sql(scripts = ["/sql/reports-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ReportControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `코스 신고는 201과 reportId를 반환하고 PENDING 상태로 저장한다`() {
            mockMvc
                .perform(report("""{"targetType":"COURSE","targetId":1,"reason":"SPAM","description":"광고 글이에요"}"""))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(1))

            val row = reportRow(1L)
            assertEquals(REPORTER_ID, row["reporter_id"])
            assertEquals("COURSE", row["target_type"])
            assertEquals(1L, row["target_id"])
            assertEquals("SPAM", row["reason"])
            assertEquals("광고 글이에요", row["description"])
            assertEquals("PENDING", row["status"])
        }

        @Test
        fun `사용자와 댓글도 신고할 수 있고 빈 설명은 null로 저장한다`() {
            mockMvc
                .perform(report("""{"targetType":"USER","targetId":2,"reason":"ABUSE"}"""))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.reportId").value(1))
            mockMvc
                .perform(
                    report("""{"targetType":"COURSE_COMMENT","targetId":1,"reason":"OTHER","description":"   "}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.reportId").value(2))

            assertEquals("USER", reportRow(1L)["target_type"])
            assertNull(reportRow(1L)["description"])
            assertEquals("COURSE_COMMENT", reportRow(2L)["target_type"])
            assertNull(reportRow(2L)["description"])
            assertReportCount(2)
        }

        @Test
        fun `없는 코스와 삭제된 코스와 볼 수 없는 비공개 코스 신고는 4041이다`() {
            for (courseId in listOf(99999L, 3L, 2L)) {
                mockMvc
                    .perform(report("""{"targetType":"COURSE","targetId":$courseId,"reason":"SPAM"}"""))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4041))
            }
            assertReportCount(0)
        }

        @Test
        fun `없는 사용자와 탈퇴한 사용자 신고는 4042이다`() {
            for (userId in listOf(99999L, 3L)) {
                mockMvc
                    .perform(report("""{"targetType":"USER","targetId":$userId,"reason":"ABUSE"}"""))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4042))
            }
            assertReportCount(0)
        }

        @Test
        fun `없는 댓글과 삭제된 댓글 신고는 4048이다`() {
            for (commentId in listOf(99999L, 3L)) {
                mockMvc
                    .perform(report("""{"targetType":"COURSE_COMMENT","targetId":$commentId,"reason":"OBSCENE"}"""))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4048))
            }
            assertReportCount(0)
        }

        @Test
        fun `본인과 본인 코스와 본인 댓글 신고는 4006이다`() {
            val bodies =
                listOf(
                    """{"targetType":"USER","targetId":$REPORTER_ID,"reason":"SPAM"}""",
                    """{"targetType":"COURSE","targetId":4,"reason":"SPAM"}""",
                    """{"targetType":"COURSE_COMMENT","targetId":2,"reason":"SPAM"}""",
                )
            for (body in bodies) {
                mockMvc
                    .perform(report(body))
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value(4006))
            }
            assertReportCount(0)
        }

        @Test
        fun `같은 대상을 다시 신고하면 4099이고 신고는 하나만 남는다`() {
            val body = """{"targetType":"COURSE","targetId":1,"reason":"SPAM"}"""
            mockMvc.perform(report(body)).andExpect(status().isCreated)
            mockMvc
                .perform(report("""{"targetType":"COURSE","targetId":1,"reason":"PRIVACY","description":"다른 사유"}"""))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4099))

            assertReportCount(1)
            assertEquals("SPAM", reportRow(1L)["reason"])
        }

        @Test
        fun `다른 종류의 대상이 같은 id면 중복이 아니다`() {
            mockMvc
                .perform(report("""{"targetType":"COURSE","targetId":1,"reason":"SPAM"}"""))
                .andExpect(status().isCreated)
            mockMvc
                .perform(report("""{"targetType":"COURSE_COMMENT","targetId":1,"reason":"SPAM"}"""))
                .andExpect(status().isCreated)
            assertReportCount(2)
        }

        @Test
        fun `500자를 초과하는 설명은 4002이다`() {
            mockMvc
                .perform(
                    report(
                        """{"targetType":"COURSE","targetId":1,"reason":"SPAM","description":"${"가".repeat(501)}"}""",
                    ),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
            assertReportCount(0)
        }

        @Test
        fun `500자 설명은 허용한다`() {
            val description = "가".repeat(500)
            mockMvc
                .perform(
                    report("""{"targetType":"COURSE","targetId":1,"reason":"SPAM","description":"$description"}"""),
                ).andExpect(status().isCreated)
            assertEquals(description, reportRow(1L)["description"])
        }

        @Test
        fun `알 수 없는 대상 종류나 사유와 누락된 필드는 400이다`() {
            val bodies =
                listOf(
                    """{"targetType":"PLACE","targetId":1,"reason":"SPAM"}""",
                    """{"targetType":"COURSE","targetId":1,"reason":"HATE"}""",
                    """{"targetType":"COURSE","reason":"SPAM"}""",
                    """{"targetId":1,"reason":"SPAM"}""",
                    """{"targetType":"COURSE","targetId":1}""",
                )
            for (body in bodies) {
                mockMvc
                    .perform(report(body))
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value(4001))
            }
            assertReportCount(0)
        }

        @Test
        fun `비로그인 사용자는 신고할 수 없다`() {
            mockMvc
                .perform(
                    post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"targetType":"COURSE","targetId":1,"reason":"SPAM"}"""),
                ).andExpect(status().isUnauthorized)
            assertReportCount(0)
        }

        private fun report(body: String) =
            post(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(REPORTER_ID)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)

        private fun reportRow(reportId: Long) = jdbcTemplate.queryForMap("SELECT * FROM reports WHERE id = ?", reportId)

        private fun assertReportCount(expected: Int) {
            assertEquals(expected, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reports", Int::class.java))
        }

        private companion object {
            const val REPORTER_ID = 1L
            const val BASE_PATH = "/api/v1/reports"
        }
    }
