package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 내 완주 목록(`GET /api/v1/my/traces?cursor&size`) 통합 테스트 — 완주만 노출·최근 시작순 커서 페이지·본인 세션 한정·입력 검증.
 * 픽스처(course-trace-fixture.sql): 사용자 1(주체)·2(타인), 코스 1(공개). 세션은 시작·종료 API 로 만든다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-trace-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseTraceListControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `완주 기록이 없으면 빈 목록이다`() {
            list()
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.nextCursor").value(null))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.traces").isEmpty)
        }

        @Test
        fun `완주한 세션만 최근 시작순으로 보이고 size 만큼 끊어 커서로 다음 페이지를 읽는다`() {
            val first = start()
            end(first, 192, 2400)
            val second = start()
            end(second, 30, 500)
            start()

            val page1 =
                list("size" to "1")
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").isNotEmpty)
                    .andExpect(jsonPath("$.data.traces.length()").value(1))
                    .andExpect(jsonPath("$.data.traces[0].traceId").value(second))
                    .andExpect(jsonPath("$.data.traces[0].course.courseId").value(1))
                    .andExpect(jsonPath("$.data.traces[0].startedAt").isNotEmpty)
                    .andExpect(jsonPath("$.data.traces[0].endedAt").isNotEmpty)
                    .andExpect(jsonPath("$.data.traces[0].durationMinutes").value(30))
                    .andExpect(jsonPath("$.data.traces[0].distanceMeters").value(500))
                    .andReturn()
                    .response.contentAsString
            val cursor = Regex("\"nextCursor\":\"([^\"]+)\"").find(page1)!!.groupValues[1]

            list("size" to "1", "cursor" to cursor)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").value(null))
                .andExpect(jsonPath("$.data.traces.length()").value(1))
                .andExpect(jsonPath("$.data.traces[0].traceId").value(first))
                .andExpect(jsonPath("$.data.traces[0].durationMinutes").value(192))
                .andExpect(jsonPath("$.data.traces[0].distanceMeters").value(2400))
        }

        @Test
        fun `코스 요약 — 제목·장소 수를 함께 내려주고 삭제 표시는 false 다`() {
            end(start(), 0, 0)

            list()
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.traces[0].course.courseId").value(1))
                .andExpect(jsonPath("$.data.traces[0].course.title").value("코스1"))
                .andExpect(jsonPath("$.data.traces[0].course.placeCount").value(3))
                .andExpect(jsonPath("$.data.traces[0].course.isDeleted").value(false))
        }

        @Test
        fun `코스 요약 — 완주 후 삭제된 코스는 기록을 남기고 courseId 외 null 에 isDeleted 가 true 다`() {
            end(start(), 192, 2400)
            jdbcTemplate.update("UPDATE courses SET deleted_at = now(), status = 'DELETED' WHERE id = 1")

            assertHiddenCourse()
        }

        @Test
        fun `코스 요약 — 완주 후 타인이 비공개로 바꾼 코스도 삭제와 같이 내려준다`() {
            end(start(), 192, 2400)
            jdbcTemplate.update("UPDATE courses SET visibility = 'PRIVATE' WHERE id = 1")

            assertHiddenCourse()
        }

        @Test
        fun `코스 요약 — 내가 비공개로 둔 내 코스는 그대로 보인다`() {
            val body =
                mockMvc
                    .perform(post("/api/v1/courses/2/traces").header(HttpHeaders.AUTHORIZATION, bearer(OTHER_USER_ID)))
                    .andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            val traceId = Regex("\"traceId\":(\\d+)").find(body)!!.groupValues[1].toLong()
            mockMvc
                .perform(
                    post("/api/v1/traces/$traceId/end")
                        .header(HttpHeaders.AUTHORIZATION, bearer(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"durationMinutes": 0, "distanceMeters": 0}"""),
                ).andExpect(status().isOk)

            mockMvc
                .perform(get("/api/v1/my/traces").header(HttpHeaders.AUTHORIZATION, bearer(OTHER_USER_ID)))
                .andExpect(jsonPath("$.data.traces[0].course.title").value("비공개코스2"))
                .andExpect(jsonPath("$.data.traces[0].course.isDeleted").value(false))
        }

        @Test
        fun `진행 중 세션은 목록에 나오지 않는다`() {
            start()

            list().andExpect(jsonPath("$.data.traces").isEmpty)
        }

        @Test
        fun `본인 세션만 보인다 — 타인은 내 완주 세션을 볼 수 없다`() {
            end(start(), 0, 0)

            mockMvc
                .perform(get("/api/v1/my/traces").header(HttpHeaders.AUTHORIZATION, bearer(OTHER_USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.traces").isEmpty)
        }

        @Test
        fun `잘못된 커서는 400 과 4001 이다`() {
            list("cursor" to "!!not-base64!!")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("잘못된 커서입니다."))
        }

        @Test
        fun `size 가 1~50 밖이면 400 과 4002 fieldErrors 다`() {
            listOf("0", "51").forEach { size ->
                list("size" to size)
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value(4002))
                    .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItems("size")))
            }
        }

        @Test
        fun `토큰 없으면 401 이다`() {
            mockMvc.perform(get("/api/v1/my/traces")).andExpect(status().isUnauthorized)
        }

        // --- helpers ---

        private fun assertHiddenCourse() {
            list()
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.traces[0].durationMinutes").value(192))
                .andExpect(jsonPath("$.data.traces[0].course.courseId").value(1))
                .andExpect(jsonPath("$.data.traces[0].course.title").value(null))
                .andExpect(jsonPath("$.data.traces[0].course.coverImageUrl").value(null))
                .andExpect(jsonPath("$.data.traces[0].course.area").value(null))
                .andExpect(jsonPath("$.data.traces[0].course.placeCount").value(null))
                .andExpect(jsonPath("$.data.traces[0].course.isDeleted").value(true))
        }

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private fun list(vararg params: Pair<String, String>): ResultActions {
            val request = get("/api/v1/my/traces").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
            params.forEach { (key, value) -> request.param(key, value) }
            return mockMvc.perform(request)
        }

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

        private fun end(
            traceId: Long,
            durationMinutes: Int,
            distanceMeters: Int,
        ) {
            mockMvc
                .perform(
                    post("/api/v1/traces/$traceId/end")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"durationMinutes": $durationMinutes, "distanceMeters": $distanceMeters}"""),
                ).andExpect(status().isOk)
        }

        private companion object {
            const val USER_ID = 1L
            const val OTHER_USER_ID = 2L
        }
    }
