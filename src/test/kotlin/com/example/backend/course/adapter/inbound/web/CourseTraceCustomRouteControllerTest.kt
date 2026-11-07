package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/**
 * 수정한 루트로 따라가기 통합 테스트 — 시작 V2(`POST /api/v2/courses/{courseId}/traces`)·루트 변경(`PATCH /api/v1/traces/{traceId}/route`)과
 * 루트 세션의 체크인·종료 병합 규칙(루트 기준 검증, 같은 장소 중복, 재전송 무시).
 * 픽스처(course-trace-fixture.sql): 사용자 1(주체)·2(타인), 코스1(공개, 장소 22·23·25)·4(공개). 24 는 코스 밖 실제 장소, 99 는 없는 장소.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-trace-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseTraceCustomRouteControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        // --- 시작 V2 ---

        @Test
        fun `시작 V2 — 받은 순서대로 PLANNED 루트 행이 생기고 응답에 placeIds 를 돌려준다`() {
            startV2(1L, "[23, 24, 22]")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("따라가기를 시작했습니다."))
                .andExpect(jsonPath("$.data.traceId").isNumber)
                .andExpect(jsonPath("$.data.courseId").value(1))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.placeIds").value(contains(23, 24, 22)))
                .andExpect(jsonPath("$.data.visitedPlaces").isEmpty)

            val traceId = inProgressTraceId()
            assertThat(tracePlaces(traceId)).containsExactly(
                TracePlace(23L, "PLANNED", 0, null),
                TracePlace(24L, "PLANNED", 1, null),
                TracePlace(22L, "PLANNED", 2, null),
            )
        }

        @Test
        fun `시작 V2 — 같은 장소를 두 번 담을 수 있다`() {
            startV2(1L, "[22, 24, 22]")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.placeIds").value(contains(22, 24, 22)))

            assertThat(tracePlaces(inProgressTraceId()).map { it.placeId }).containsExactly(22L, 24L, 22L)
        }

        @Test
        fun `시작 V2 — 같은 코스를 진행 중이면 요청 루트는 무시하고 기존 세션과 그 루트를 돌려준다`() {
            val traceId = startV2Id("[23, 24]")
            checkIn(traceId, 24L, "2026-10-10T05:00:00Z").andExpect(status().isOk)

            startV2(1L, "[22]")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.traceId").value(traceId))
                .andExpect(jsonPath("$.data.placeIds").value(contains(23, 24)))
                .andExpect(jsonPath("$.data.visitedPlaces[0].placeId").value(24))
                .andExpect(jsonPath("$.data.visitedPlaces[0].visitedAt").value("2026-10-10T05:00:00Z"))

            assertThat(tracePlaces(traceId).map { it.placeId }).containsExactly(23L, 24L)
        }

        @Test
        fun `시작 V2 — V1 으로 시작한 세션을 이어가면 코스 장소를 placeIds 로 돌려주고 루트 행은 만들지 않는다`() {
            val traceId = startV1()

            startV2(1L, "[24]")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.traceId").value(traceId))
                .andExpect(jsonPath("$.data.placeIds").value(contains(22, 23, 25)))

            assertThat(tracePlaces(traceId)).isEmpty()
        }

        @Test
        fun `시작 V2 — 다른 코스를 진행 중이면 409 와 4100 이다`() {
            startV2Id("[22]")

            startV2(4L, "[22]")
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4100))
        }

        @Test
        fun `시작 V2 — 없는 장소가 있으면 404 와 4043 이고 세션을 만들지 않는다`() {
            startV2(1L, "[22, 99]")
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4043))

            assertThat(traceCount()).isZero()
        }

        @Test
        fun `시작 V2 — 없는 코스는 404 와 4041 이다`() {
            startV2(999L, "[22]")
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `시작 V2 — placeIds 가 비었거나 20개를 넘으면 400 과 4002 fieldErrors 다`() {
            listOf("[]", (1..21).joinToString(prefix = "[", postfix = "]") { "22" }).forEach { placeIds ->
                startV2(1L, placeIds)
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value(4002))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("placeIds"))
            }
            assertThat(traceCount()).isZero()
        }

        @Test
        fun `시작 V2 — 토큰 없으면 401 이다`() {
            mockMvc
                .perform(
                    post("/api/v2/courses/1/traces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"placeIds": [22]}"""),
                ).andExpect(status().isUnauthorized)
        }

        // --- 루트 세션의 체크인·종료 ---

        @Test
        fun `체크인 — 루트 세션은 코스 장소가 아니라 루트 기준이라 코스 밖 장소는 되고 루트에서 뺀 코스 장소는 400 과 4005 다`() {
            val traceId = startV2Id("[22, 24]")

            checkIn(traceId, 24L, "2026-10-10T05:00:00Z").andExpect(status().isOk)
            checkIn(traceId, 23L, "2026-10-10T05:10:00Z")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))

            assertThat(tracePlaces(traceId)).containsExactly(
                TracePlace(22L, "PLANNED", 0, null),
                TracePlace(24L, "VISITED", 1, Instant.parse("2026-10-10T05:00:00Z")),
            )
        }

        @Test
        fun `체크인 — 같은 장소·같은 시각은 재전송이라 무시하고 시각이 다르면 다음 항목을 방문 처리하며 담긴 횟수를 넘으면 무시한다`() {
            val traceId = startV2Id("[22, 24, 22]")

            checkIn(traceId, 22L, "2026-10-10T05:00:00Z").andExpect(status().isOk)
            checkIn(traceId, 22L, "2026-10-10T05:00:00Z").andExpect(status().isOk)
            assertThat(tracePlaces(traceId).map { it.status }).containsExactly("VISITED", "PLANNED", "PLANNED")

            checkIn(traceId, 22L, "2026-10-10T06:00:00Z").andExpect(status().isOk)
            checkIn(traceId, 22L, "2026-10-10T07:00:00Z").andExpect(status().isOk)
            assertThat(tracePlaces(traceId)).containsExactly(
                TracePlace(22L, "VISITED", 0, Instant.parse("2026-10-10T05:00:00Z")),
                TracePlace(24L, "PLANNED", 1, null),
                TracePlace(22L, "VISITED", 2, Instant.parse("2026-10-10T06:00:00Z")),
            )
        }

        @Test
        fun `종료 — 병합한 체크인도 재전송은 걸러지고 새 방문만 루트 항목에 기록되며 루트 밖 장소는 400 과 4005 다`() {
            val traceId = startV2Id("[22, 24, 22]")
            checkIn(traceId, 22L, "2026-10-10T05:00:00Z").andExpect(status().isOk)

            end(
                traceId,
                """{"visitedPlaces": [{"placeId": 25, "visitedAt": "2026-10-10T06:00:00Z"}], "durationMinutes": 1, "distanceMeters": 1}""",
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))
            assertThat(traceStatus(traceId)).isEqualTo("IN_PROGRESS")

            end(
                traceId,
                """
                {
                  "visitedPlaces": [
                    { "placeId": 22, "visitedAt": "2026-10-10T05:00:00.000Z" },
                    { "placeId": 22, "visitedAt": "2026-10-10T06:00:00Z" }
                  ],
                  "durationMinutes": 90,
                  "distanceMeters": 1500
                }
                """.trimIndent(),
            ).andExpect(status().isOk)

            assertThat(traceStatus(traceId)).isEqualTo("COMPLETED")
            assertThat(tracePlaces(traceId)).containsExactly(
                TracePlace(22L, "VISITED", 0, Instant.parse("2026-10-10T05:00:00Z")),
                TracePlace(24L, "PLANNED", 1, null),
                TracePlace(22L, "VISITED", 2, Instant.parse("2026-10-10T06:00:00Z")),
            )
        }

        // --- 루트 변경 ---

        @Test
        fun `루트 변경 — 체크인한 장소는 방문 순서로 앞에 남고 그 뒤가 받은 장소로 바뀐다`() {
            val traceId = startV2Id("[22, 23, 24]")
            checkIn(traceId, 23L, "2026-10-10T05:00:00Z").andExpect(status().isOk)
            checkIn(traceId, 22L, "2026-10-10T04:00:00Z").andExpect(status().isOk)

            changeRoute(traceId, "[24, 22]")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("따라가는 루트를 변경했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())

            assertThat(tracePlaces(traceId)).containsExactly(
                TracePlace(22L, "VISITED", 0, Instant.parse("2026-10-10T04:00:00Z")),
                TracePlace(23L, "VISITED", 1, Instant.parse("2026-10-10T05:00:00Z")),
                TracePlace(24L, "PLANNED", 2, null),
                TracePlace(22L, "PLANNED", 3, null),
            )
            // 체크인한 장소는 루트에 남아 다시 보내도 200 이지만 더 기록되지 않고, 다시 담은 장소는 한 번 더 방문할 수 있다.
            checkIn(traceId, 23L, "2026-10-10T06:00:00Z").andExpect(status().isOk)
            checkIn(traceId, 22L, "2026-10-10T06:00:00Z").andExpect(status().isOk)
            assertThat(
                tracePlaces(traceId).map { it.status },
            ).containsExactly("VISITED", "VISITED", "PLANNED", "VISITED")
            assertThat(tracePlaces(traceId)[1].visitedAt).isEqualTo(Instant.parse("2026-10-10T05:00:00Z"))
        }

        @Test
        fun `루트 변경 — V1 으로 시작한 세션도 바꿀 수 있고 그 뒤로는 루트 기준으로 검증한다`() {
            val traceId = startV1()
            checkIn(traceId, 25L, "2026-10-10T05:00:00Z").andExpect(status().isOk)

            changeRoute(traceId, "[24]").andExpect(status().isOk)

            assertThat(tracePlaces(traceId)).containsExactly(
                TracePlace(25L, "VISITED", 0, Instant.parse("2026-10-10T05:00:00Z")),
                TracePlace(24L, "PLANNED", 1, null),
            )
            checkIn(traceId, 22L, "2026-10-10T06:00:00Z")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))
        }

        @Test
        fun `루트 변경 — 체크인한 곳이 있으면 빈 목록으로 남은 장소를 모두 뺄 수 있고 없으면 400 과 4001 이다`() {
            val traceId = startV2Id("[22, 24]")

            changeRoute(traceId, "[]")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
            assertThat(tracePlaces(traceId).map { it.placeId }).containsExactly(22L, 24L)

            checkIn(traceId, 22L, "2026-10-10T05:00:00Z").andExpect(status().isOk)
            changeRoute(traceId, "[]").andExpect(status().isOk)
            assertThat(tracePlaces(traceId)).containsExactly(
                TracePlace(22L, "VISITED", 0, Instant.parse("2026-10-10T05:00:00Z")),
            )
        }

        @Test
        fun `루트 변경 — 없는 장소는 404 와 4043 이고 루트는 그대로다`() {
            val traceId = startV2Id("[22, 24]")

            changeRoute(traceId, "[23, 99]")
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4043))

            assertThat(tracePlaces(traceId).map { it.placeId }).containsExactly(22L, 24L)
        }

        @Test
        fun `루트 변경 — 20개를 넘으면 400 과 4002 fieldErrors 다`() {
            val traceId = startV2Id("[22]")

            changeRoute(traceId, (1..21).joinToString(prefix = "[", postfix = "]") { "24" })
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("placeIds"))
        }

        @Test
        fun `루트 변경 — 타인의 세션이나 없는 세션은 404 와 4049 다`() {
            val traceId = startV2Id("[22]")

            listOf(USER_ID to 99999L, OTHER_USER_ID to traceId).forEach { (userId, id) ->
                mockMvc
                    .perform(
                        patch("/api/v1/traces/$id/route")
                            .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"placeIds": [24]}"""),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4049))
            }
            assertThat(tracePlaces(traceId).map { it.placeId }).containsExactly(22L)
        }

        @Test
        fun `루트 변경 — 종료된 세션은 409 와 4099 다`() {
            val traceId = startV2Id("[22]")
            end(traceId, """{"durationMinutes": 0, "distanceMeters": 0}""").andExpect(status().isOk)

            changeRoute(traceId, "[24]")
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4099))
        }

        @Test
        fun `루트 변경 — 토큰 없으면 401 이다`() {
            mockMvc
                .perform(
                    patch("/api/v1/traces/1/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"placeIds": [24]}"""),
                ).andExpect(status().isUnauthorized)
        }

        // --- helpers ---

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private fun startV2(
            courseId: Long,
            placeIds: String,
        ): ResultActions =
            mockMvc.perform(
                post("/api/v2/courses/$courseId/traces")
                    .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"placeIds": $placeIds}"""),
            )

        /** 코스 1 을 [placeIds] 루트로 시작하고 traceId 를 돌려준다. */
        private fun startV2Id(placeIds: String): Long = traceIdOf(startV2(1L, placeIds).andExpect(status().isOk))

        /** 코스 1 을 V1(코스 장소 그대로)으로 시작하고 traceId 를 돌려준다. */
        private fun startV1(): Long =
            traceIdOf(
                mockMvc
                    .perform(post("/api/v1/courses/1/traces").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                    .andExpect(status().isOk),
            )

        private fun traceIdOf(result: ResultActions): Long =
            Regex("\"traceId\":(\\d+)").find(result.andReturn().response.contentAsString)!!.groupValues[1].toLong()

        private fun checkIn(
            traceId: Long,
            placeId: Long,
            visitedAt: String,
        ): ResultActions =
            mockMvc.perform(
                post("/api/v1/traces/$traceId/places")
                    .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"placeId": $placeId, "visitedAt": "$visitedAt"}"""),
            )

        private fun changeRoute(
            traceId: Long,
            placeIds: String,
        ): ResultActions =
            mockMvc.perform(
                patch("/api/v1/traces/$traceId/route")
                    .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"placeIds": $placeIds}"""),
            )

        private fun end(
            traceId: Long,
            body: String,
        ): ResultActions =
            mockMvc.perform(
                post("/api/v1/traces/$traceId/end")
                    .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )

        private fun inProgressTraceId(): Long =
            jdbcTemplate.queryForObject(
                "SELECT id FROM tracing_courses WHERE user_id = ? AND status = 'IN_PROGRESS'",
                Long::class.java,
                USER_ID,
            )!!

        private fun traceCount(): Int =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tracing_courses WHERE user_id = ?",
                Int::class.java,
                USER_ID,
            )!!

        private fun traceStatus(traceId: Long): String =
            jdbcTemplate.queryForObject(
                "SELECT status FROM tracing_courses WHERE id = ?",
                String::class.java,
                traceId,
            )!!

        /** 세션의 장소 행을 루트 순서(order_no, 없으면 id)로 읽는다. */
        private fun tracePlaces(traceId: Long): List<TracePlace> =
            jdbcTemplate.query(
                "SELECT place_id, status, order_no, visited_at FROM tracing_course_places WHERE tracing_course_id = ? ORDER BY order_no, id",
                { rs, _ ->
                    TracePlace(
                        placeId = rs.getLong("place_id"),
                        status = rs.getString("status"),
                        orderNo = rs.getObject("order_no")?.let { (it as Number).toInt() },
                        visitedAt = rs.getTimestamp("visited_at")?.toInstant(),
                    )
                },
                traceId,
            )

        private data class TracePlace(
            val placeId: Long,
            val status: String,
            val orderNo: Int?,
            val visitedAt: Instant?,
        )

        private companion object {
            const val USER_ID = 1L
            const val OTHER_USER_ID = 2L
        }
    }
