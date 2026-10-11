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
 * 따라가기 컨트롤러 통합 테스트 — 시작(`POST /api/v1/courses/{courseId}/traces`)·체크인(`POST /api/v1/traces/{traceId}/places`)·
 * 종료(`POST /api/v1/traces/{traceId}/end`).
 * 픽스처(course-trace-fixture.sql): 사용자 1(주체)·2(타인), 코스1(공개, 장소 22·23·25)·2(타인 비공개)·3(소프트 삭제).
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
        // --- 시작 ---

        @Test
        fun `시작 — 200 과 세션(traceId·startedAt·빈 visitedPlaces)을 내려주고 IN_PROGRESS 행이 생긴다`() {
            mockMvc
                .perform(post("/api/v1/courses/1/traces").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("따라가기를 시작했습니다."))
                .andExpect(jsonPath("$.data.traceId").value(1))
                .andExpect(jsonPath("$.data.courseId").value(1))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.visitedPlaces").isEmpty)

            assertThat(traceStatus(1L)).isEqualTo("IN_PROGRESS")
            assertThat(tracingsCnt(1L)).isZero()
        }

        @Test
        fun `시작 — 진행 중 세션이 있으면 새로 만들지 않고 그 세션과 체크인 목록을 돌려준다(이어가기)`() {
            val traceId = start()
            checkIn(traceId, 22L, "2026-10-10T05:00:00Z").andExpect(status().isOk)

            mockMvc
                .perform(post("/api/v1/courses/1/traces").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.traceId").value(traceId))
                .andExpect(jsonPath("$.data.visitedPlaces.length()").value(1))
                .andExpect(jsonPath("$.data.visitedPlaces[0].placeId").value(22))
                .andExpect(jsonPath("$.data.visitedPlaces[0].visitedAt").value("2026-10-10T05:00:00Z"))

            assertThat(traceCount(USER_ID, 1L)).isEqualTo(1)
        }

        @Test
        fun `시작 — 종료한 뒤 다시 시작하면 새 세션이 생긴다`() {
            val first = start()
            end(first, """{"durationMinutes": 0, "distanceMeters": 0}""").andExpect(status().isOk)

            val second = start()

            assertThat(second).isNotEqualTo(first)
            assertThat(traceStatus(first)).isEqualTo("COMPLETED")
            assertThat(traceStatus(second)).isEqualTo("IN_PROGRESS")
        }

        @Test
        fun `시작 — 없는 코스·타인 비공개 코스·삭제된 코스는 404 와 4041 이다`() {
            listOf(99999L, 2L, 3L).forEach { courseId ->
                mockMvc
                    .perform(
                        post("/api/v1/courses/$courseId/traces").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4041))
            }
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tracing_courses", Int::class.java)).isZero()
        }

        @Test
        fun `시작 — 토큰 없으면 401 이다`() {
            mockMvc.perform(post("/api/v1/courses/1/traces")).andExpect(status().isUnauthorized)
        }

        // --- 체크인 ---

        @Test
        fun `체크인 — 방문 장소가 체크인 순서대로 쌓이고 같은 장소 재전송은 200 이지만 중복되지 않는다`() {
            val traceId = start()

            checkIn(traceId, 22L, "2026-10-10T05:00:00Z")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("장소를 방문 처리했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
            checkIn(traceId, 23L, "2026-10-10T06:00:00Z").andExpect(status().isOk)
            checkIn(traceId, 22L, "2026-10-10T06:30:00Z").andExpect(status().isOk)

            val places = visitedPlaces(traceId)
            assertThat(places.map { it["place_id"] }).containsExactly(22L, 23L)
            assertThat((places[0]["visited_at"] as java.sql.Timestamp).toInstant())
                .isEqualTo(java.time.Instant.parse("2026-10-10T05:00:00Z"))
        }

        @Test
        fun `체크인 — 타인의 세션이나 없는 세션은 404 와 4049 다`() {
            val traceId = start()

            listOf(USER_ID to 99999L, OTHER_USER_ID to traceId).forEach { (userId, id) ->
                mockMvc
                    .perform(
                        post("/api/v1/traces/$id/places")
                            .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"placeId": 22, "visitedAt": "2026-10-10T05:00:00Z"}"""),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4049))
            }
            assertThat(visitedPlaces(traceId)).isEmpty()
        }

        @Test
        fun `체크인 — 종료된 세션은 409 와 4099 다`() {
            val traceId = start()
            end(traceId, """{"durationMinutes": 0, "distanceMeters": 0}""").andExpect(status().isOk)

            checkIn(traceId, 22L, "2026-10-10T05:00:00Z")
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4099))
        }

        @Test
        fun `체크인 — 코스에 없는 장소는 400 과 4005 이고 기록되지 않는다`() {
            val traceId = start()

            checkIn(traceId, 24L, "2026-10-10T05:00:00Z")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))

            assertThat(visitedPlaces(traceId)).isEmpty()
        }

        @Test
        fun `체크인 — 0 이하 placeId 는 400 과 4002 fieldErrors 다`() {
            val traceId = start()

            checkIn(traceId, 0L, "2026-10-10T05:00:00Z")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItems("placeId")))
        }

        // --- 종료 ---

        @Test
        fun `종료 — 소요 시간·이동 거리를 기록하고 COMPLETED 로 바꾸며 미전송 체크인을 합치고 tracings_cnt 가 오른다`() {
            val traceId = start()
            checkIn(traceId, 22L, "2026-10-10T05:00:00Z").andExpect(status().isOk)

            end(
                traceId,
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
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("따라가기를 종료했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())

            val trace =
                jdbcTemplate.queryForMap(
                    "SELECT status, duration_minutes, distance_meters, ended_at FROM tracing_courses WHERE id = ?",
                    traceId,
                )
            assertThat(trace["status"]).isEqualTo("COMPLETED")
            assertThat(trace["duration_minutes"]).isEqualTo(192)
            assertThat(trace["distance_meters"]).isEqualTo(2400)
            assertThat(trace["ended_at"]).isNotNull()
            assertThat(visitedPlaces(traceId).map { it["place_id"] }).containsExactly(22L, 25L)
            assertThat(tracingsCnt(1L)).isEqualTo(1)
        }

        @Test
        fun `종료 — 체크인 병합 없이(visitedPlaces 생략) 종료해도 200 이다`() {
            val traceId = start()

            end(traceId, """{"durationMinutes": 0, "distanceMeters": 0}""")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            assertThat(traceStatus(traceId)).isEqualTo("COMPLETED")
            assertThat(visitedPlaces(traceId)).isEmpty()
        }

        @Test
        fun `종료 — 이미 종료된 세션을 다시 종료하면 409 와 4099 이고 tracings_cnt 는 그대로다`() {
            val traceId = start()
            end(traceId, """{"durationMinutes": 0, "distanceMeters": 0}""").andExpect(status().isOk)

            end(traceId, """{"durationMinutes": 10, "distanceMeters": 10}""")
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4099))

            assertThat(tracingsCnt(1L)).isEqualTo(1)
        }

        @Test
        fun `종료 — 타인의 세션이나 없는 세션은 404 와 4049 다`() {
            val traceId = start()

            listOf(USER_ID to 99999L, OTHER_USER_ID to traceId).forEach { (userId, id) ->
                mockMvc
                    .perform(
                        post("/api/v1/traces/$id/end")
                            .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"durationMinutes": 0, "distanceMeters": 0}"""),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4049))
            }
            assertThat(traceStatus(traceId)).isEqualTo("IN_PROGRESS")
        }

        @Test
        fun `종료 — 병합할 장소 중 코스에 없는 것이 있으면 400 과 4005 이고 세션은 진행 중 그대로다`() {
            val traceId = start()

            end(
                traceId,
                """
                {
                  "visitedPlaces": [
                    { "placeId": 22, "visitedAt": "2026-10-10T05:00:00Z" },
                    { "placeId": 24, "visitedAt": "2026-10-10T06:00:00Z" }
                  ],
                  "durationMinutes": 10,
                  "distanceMeters": 10
                }
                """.trimIndent(),
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))

            assertThat(traceStatus(traceId)).isEqualTo("IN_PROGRESS")
            assertThat(visitedPlaces(traceId)).isEmpty()
            assertThat(tracingsCnt(1L)).isZero()
        }

        @Test
        fun `종료 — 음수 소요 시간·0 이하 placeId 는 400 과 4002 fieldErrors 다`() {
            val traceId = start()

            end(
                traceId,
                """
                {
                  "visitedPlaces": [ { "placeId": 0, "visitedAt": "2026-10-10T05:00:00Z" } ],
                  "durationMinutes": -1,
                  "distanceMeters": 2400
                }
                """.trimIndent(),
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(
                    jsonPath("$.fieldErrors[*].field").value(hasItems("durationMinutes", "visitedPlaces[0].placeId")),
                )
        }

        @Test
        fun `종료 — 필수 필드(durationMinutes) 누락은 400 과 4001 이다`() {
            val traceId = start()

            end(traceId, """{"distanceMeters": 0}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `종료 — 토큰 없으면 401 이다`() {
            mockMvc
                .perform(
                    post("/api/v1/traces/1/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"durationMinutes": 0, "distanceMeters": 0}"""),
                ).andExpect(status().isUnauthorized)
        }

        // --- helpers ---

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        /** 코스 1 따라가기를 시작하고 traceId 를 돌려준다. */
        private fun start(): Long {
            val body =
                mockMvc
                    .perform(post("/api/v1/courses/1/traces").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                    .andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            return Regex("\"traceId\":(\\d+)").find(body)!!.groupValues[1].toLong()
        }

        private fun checkIn(
            traceId: Long,
            placeId: Long,
            visitedAt: String,
        ) = mockMvc.perform(
            post("/api/v1/traces/$traceId/places")
                .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"placeId": $placeId, "visitedAt": "$visitedAt"}"""),
        )

        private fun end(
            traceId: Long,
            body: String,
        ) = mockMvc.perform(
            post("/api/v1/traces/$traceId/end")
                .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )

        private fun traceStatus(traceId: Long): String =
            jdbcTemplate.queryForObject(
                "SELECT status FROM tracing_courses WHERE id = ?",
                String::class.java,
                traceId,
            )!!

        private fun traceCount(
            userId: Long,
            courseId: Long,
        ): Int =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tracing_courses WHERE user_id = ? AND course_id = ?",
                Int::class.java,
                userId,
                courseId,
            )!!

        private fun visitedPlaces(traceId: Long): List<Map<String, Any?>> =
            jdbcTemplate.queryForList(
                "SELECT place_id, visited_at FROM tracing_course_places WHERE tracing_course_id = ? ORDER BY id",
                traceId,
            )

        private fun tracingsCnt(courseId: Long): Int =
            jdbcTemplate.queryForObject("SELECT tracings_cnt FROM courses WHERE id = ?", Int::class.java, courseId)!!

        private companion object {
            const val USER_ID = 1L
            const val OTHER_USER_ID = 2L
        }
    }
