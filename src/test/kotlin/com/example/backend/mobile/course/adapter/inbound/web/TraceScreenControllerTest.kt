package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
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
 * 따라가기 상세 화면 조합(`GET /service/v1/my/traces/{traceId}`) 통합 테스트.
 * 픽스처(course-trace-fixture.sql): 사용자 1(주체)·2(타인), 코스 1(공개, 장소 22·23·25 — 25 는 장소 행이 없어 삭제된 장소 역할), 24 는 코스 밖 실제 장소.
 * 세션은 시작(V1·V2)·체크인·종료 API 로 만든다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-trace-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TraceScreenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `세션에 코스 요약·코스 장소·장소 요약·체크인 시각을 붙여 내려준다`() {
            val traceId = start()
            checkIn(traceId, 22L, "2026-10-10T05:00:00Z")

            screen(traceId, USER_ID)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.traceId").value(traceId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.endedAt").value(null))
                .andExpect(jsonPath("$.data.durationMinutes").value(0))
                .andExpect(jsonPath("$.data.placeCount").value(3))
                .andExpect(jsonPath("$.data.visitedPlaceCount").value(1))
                .andExpect(jsonPath("$.data.course.courseId").value(1))
                .andExpect(jsonPath("$.data.course.title").value("코스1"))
                .andExpect(jsonPath("$.data.course.isDeleted").value(false))
                .andExpect(jsonPath("$.data.places.length()").value(3))
                // 첫 장소 — 코스 쪽 순서 + 장소 쪽 이름·카테고리·주소·좌표 + 체크인 시각
                .andExpect(jsonPath("$.data.places[0].placeId").value(22))
                .andExpect(jsonPath("$.data.places[0].orderNo").value(0))
                .andExpect(jsonPath("$.data.places[0].name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.places[0].categories[0]").value("CAFE"))
                .andExpect(jsonPath("$.data.places[0].address").value("서울 성동구 아차산로 100"))
                .andExpect(jsonPath("$.data.places[0].location.latitude").value(37.5446))
                .andExpect(jsonPath("$.data.places[0].visitedAt").value("2026-10-10T05:00:00Z"))
                // 아직 방문하지 않은 장소는 visitedAt 이 없다
                .andExpect(jsonPath("$.data.places[1].placeId").value(23))
                .andExpect(jsonPath("$.data.places[1].visitedAt").value(null))
                // 삭제된 장소는 자리는 지키되 요약이 없다
                .andExpect(jsonPath("$.data.places[2].placeId").value(25))
                .andExpect(jsonPath("$.data.places[2].name").value(null))
                .andExpect(jsonPath("$.data.places[2].categories").isEmpty)
                .andExpect(jsonPath("$.data.places[2].location").value(null))
        }

        @Test
        fun `코스가 삭제·비공개돼도 장소는 그대로 내려주고 course 는 isDeleted 다`() {
            val traceId = start()
            listOf(
                "UPDATE courses SET deleted_at = now(), status = 'DELETED' WHERE id = 1",
                "UPDATE courses SET deleted_at = null, status = 'ACTIVE', visibility = 'PRIVATE' WHERE id = 1",
            ).forEach { sql ->
                jdbcTemplate.update(sql)

                screen(traceId, USER_ID)
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.data.course.courseId").value(1))
                    .andExpect(jsonPath("$.data.course.title").value(null))
                    .andExpect(jsonPath("$.data.course.coverImageUrl").value(null))
                    .andExpect(jsonPath("$.data.course.isDeleted").value(true))
                    .andExpect(jsonPath("$.data.placeCount").value(3))
                    .andExpect(jsonPath("$.data.places[0].name").value("어니언 성수"))
            }
        }

        @Test
        fun `타인의 세션이나 없는 세션은 404 와 4049 다`() {
            val traceId = start()

            screen(traceId, OTHER_USER_ID).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value(4049))
            screen(99999L, USER_ID).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value(4049))
        }

        @Test
        fun `완주한 세션은 종료 시각·소요 시간·거리와 함께 그대로 조회된다 — 완주 목록에서 하나씩 볼 수 있다`() {
            val traceId = start()
            checkIn(traceId, 22L, "2026-10-10T05:00:00Z")
            mockMvc
                .perform(
                    post("/api/v1/traces/$traceId/end")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"durationMinutes": 192, "distanceMeters": 2400}"""),
                ).andExpect(status().isOk)

            screen(traceId, USER_ID)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.endedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.durationMinutes").value(192))
                .andExpect(jsonPath("$.data.distanceMeters").value(2400))
                .andExpect(jsonPath("$.data.visitedPlaceCount").value(1))
                .andExpect(jsonPath("$.data.places[0].visitedAt").value("2026-10-10T05:00:00Z"))
        }

        @Test
        fun `루트를 수정해 시작한 세션은 루트 순서대로 장소를 내려주고 코스 밖 장소는 캡션·사진이 없다`() {
            val traceId = startV2("[24, 22]")

            screen(traceId, USER_ID)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.placeCount").value(2))
                .andExpect(jsonPath("$.data.places.length()").value(2))
                .andExpect(jsonPath("$.data.places[0].placeId").value(24))
                .andExpect(jsonPath("$.data.places[0].orderNo").value(0))
                .andExpect(jsonPath("$.data.places[0].caption").value(null))
                .andExpect(jsonPath("$.data.places[0].images").isEmpty)
                .andExpect(jsonPath("$.data.places[0].name").value("성수 연방"))
                .andExpect(jsonPath("$.data.places[0].location.longitude").value(127.057))
                .andExpect(jsonPath("$.data.places[1].placeId").value(22))
                .andExpect(jsonPath("$.data.places[1].orderNo").value(1))
                .andExpect(jsonPath("$.data.places[1].name").value("어니언 성수"))
        }

        @Test
        fun `같은 장소가 여러 번 담긴 루트는 n 번째 항목에 그 장소의 n 번째 방문 시각을 붙인다`() {
            val traceId = startV2("[22, 24, 22]")
            checkIn(traceId, 22L, "2026-10-10T05:00:00Z")
            checkIn(traceId, 22L, "2026-10-10T06:00:00Z")

            screen(traceId, USER_ID)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.placeCount").value(3))
                .andExpect(jsonPath("$.data.visitedPlaceCount").value(2))
                .andExpect(jsonPath("$.data.places[0].placeId").value(22))
                .andExpect(jsonPath("$.data.places[0].visitedAt").value("2026-10-10T05:00:00Z"))
                .andExpect(jsonPath("$.data.places[1].placeId").value(24))
                .andExpect(jsonPath("$.data.places[1].visitedAt").value(null))
                .andExpect(jsonPath("$.data.places[2].placeId").value(22))
                .andExpect(jsonPath("$.data.places[2].visitedAt").value("2026-10-10T06:00:00Z"))
        }

        @Test
        fun `mock=true 면 조회 없이 고정 목을 돌려준다`() {
            mockMvc
                .perform(
                    get("/service/v1/my/traces/999999")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.traceId").value(10))
                .andExpect(jsonPath("$.data.course.title").value("을지로 노포 골목 투어"))
                .andExpect(jsonPath("$.data.places.length()").value(2))
        }

        @Test
        fun `토큰 없으면 401 이다`() {
            mockMvc.perform(get("/service/v1/my/traces/1")).andExpect(status().isUnauthorized)
        }

        // --- helpers ---

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private fun screen(
            traceId: Long,
            userId: Long,
        ): ResultActions =
            mockMvc.perform(
                get("/service/v1/my/traces/$traceId").header(HttpHeaders.AUTHORIZATION, bearer(userId)),
            )

        private fun start(): Long {
            val body =
                mockMvc
                    .perform(post("/api/v1/courses/1/traces").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                    .andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            return Regex("\"traceId\":(\\d+)").find(body)!!.groupValues[1].toLong()
        }

        /** 코스 1 을 [placeIds] 루트로 시작하고 traceId 를 돌려준다. */
        private fun startV2(placeIds: String): Long {
            val body =
                mockMvc
                    .perform(
                        post("/api/v2/courses/1/traces")
                            .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"placeIds": $placeIds}"""),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            return Regex("\"traceId\":(\\d+)").find(body)!!.groupValues[1].toLong()
        }

        private fun checkIn(
            traceId: Long,
            placeId: Long,
            visitedAt: String,
        ) {
            mockMvc
                .perform(
                    post("/api/v1/traces/$traceId/places")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"placeId": $placeId, "visitedAt": "$visitedAt"}"""),
                ).andExpect(status().isOk)
        }

        private companion object {
            const val USER_ID = 1L
            const val OTHER_USER_ID = 2L
        }
    }
