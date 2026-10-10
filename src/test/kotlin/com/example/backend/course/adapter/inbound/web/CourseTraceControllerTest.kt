package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItems
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

/**
 * 따라가기 종료 컨트롤러(`POST /api/v1/courses/{courseId}/trace`) 통합 테스트.
 * 픽스처(course-trace-fixture.sql): 사용자 1(주체)·2(타인), 코스1(공개)·2(타인 비공개)·3(소프트 삭제).
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-trace-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseTraceControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `종료 — 방문 기록과 소요 시간·이동 거리를 보내면 200 과 안내 메시지를 내려주고 기록·tracings_cnt 가 반영된다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "visitedPlaces": [
                                { "placeId": 22, "visitedAt": "2026-10-10T05:00:00Z" },
                                { "placeId": 25, "visitedAt": "2026-10-10T07:48:00Z" }
                              ],
                              "durationMinutes": 192,
                              "distanceMeters": 2400
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("따라가기를 종료했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())

            assertThat(tracingCount(USER_ID, 1L)).isEqualTo(1)
            assertThat(tracingsCnt(1L)).isEqualTo(1)
            val trace =
                jdbcTemplate.queryForMap(
                    "SELECT id, duration_minutes, distance_meters FROM tracing_courses WHERE user_id = ? AND course_id = ?",
                    USER_ID,
                    1L,
                )
            assertThat(trace["duration_minutes"]).isEqualTo(192)
            assertThat(trace["distance_meters"]).isEqualTo(2400)
            val places =
                jdbcTemplate.queryForList(
                    "SELECT place_id, order_no, visited_at FROM tracing_course_places WHERE tracing_course_id = ? ORDER BY order_no",
                    trace["id"],
                )
            assertThat(places.map { it["place_id"] }).containsExactly(22L, 25L)
            assertThat(places.map { (it["order_no"] as Number).toInt() }).containsExactly(0, 1)
            assertThat((places[1]["visited_at"] as java.sql.Timestamp).toInstant())
                .isEqualTo(java.time.Instant.parse("2026-10-10T07:48:00Z"))
        }

        @Test
        fun `종료 — 같은 코스를 다시 종료하면 기록이 한 행 더 쌓이고 tracings_cnt 도 2 가 된다`() {
            repeat(2) {
                mockMvc
                    .perform(
                        post("/api/v1/courses/1/trace")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"visitedPlaces": [], "durationMinutes": 0, "distanceMeters": 0}"""),
                    ).andExpect(status().isOk)
            }

            assertThat(tracingCount(USER_ID, 1L)).isEqualTo(2)
            assertThat(tracingsCnt(1L)).isEqualTo(2)
        }

        @Test
        fun `종료 — 없는 코스·타인 비공개 코스·삭제된 코스는 404 와 4041 이다`() {
            listOf(99999L, 2L, 3L).forEach { courseId ->
                mockMvc
                    .perform(
                        post("/api/v1/courses/$courseId/trace")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"visitedPlaces": [], "durationMinutes": 0, "distanceMeters": 0}"""),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4041))
            }
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tracing_courses", Int::class.java)).isZero()
        }

        @Test
        fun `종료 — 방문이 없어도(빈 배열) 200 이다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"visitedPlaces": [], "durationMinutes": 0, "distanceMeters": 0}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
        }

        @Test
        fun `종료 — 음수 소요 시간·0 이하 placeId 는 400 과 4002 fieldErrors 다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "visitedPlaces": [ { "placeId": 0, "visitedAt": "2026-10-10T05:00:00Z" } ],
                              "durationMinutes": -1,
                              "distanceMeters": 2400
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(
                    jsonPath("$.fieldErrors[*].field").value(hasItems("durationMinutes", "visitedPlaces[0].placeId")),
                )
        }

        @Test
        fun `종료 — 필수 필드(durationMinutes) 누락은 400 과 4001 이다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"visitedPlaces": [], "distanceMeters": 0}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `종료 — 토큰 없으면 401 이다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"visitedPlaces": [], "durationMinutes": 0, "distanceMeters": 0}"""),
                ).andExpect(status().isUnauthorized)
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private fun tracingCount(
            userId: Long,
            courseId: Long,
        ): Int =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tracing_courses WHERE user_id = ? AND course_id = ?",
                Int::class.java,
                userId,
                courseId,
            )!!

        private fun tracingsCnt(courseId: Long): Int =
            jdbcTemplate.queryForObject("SELECT tracings_cnt FROM courses WHERE id = ?", Int::class.java, courseId)!!

        private companion object {
            const val USER_ID = 1L
        }
    }
