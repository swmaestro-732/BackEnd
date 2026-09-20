package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.domain.model.SocialProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 코스 계획 CRUD(`/api/v1/plans`) 통합 테스트.
 * 픽스처(plan-fixture.sql)는 계획을 비우고 사용자 1·2, 장소 1~3, 원본 코스 901 만 둔다.
 *
 * 저장 결과(본문·장소 순서·메모 정규화)는 DB 를 직접 읽어 확인하고, 입력 검증은 웹 DTO(4002 fieldErrors)와
 * 도메인 불변식(4001)이 각각 어디서 걸리는지까지 본다. 조회·편집·삭제는 소유자 은닉(없음·타인 동일 4047)과
 * 소프트 삭제를 본다. 목록은 계획을 미리 심은 별도 픽스처로 커서 페이지를 검증한다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/plan-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlanControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `계획을 만들면 201과 planId 를 내려주고 본문·장소가 저장된다`() {
            mockMvc
                .perform(
                    createRequest(
                        """
                        {
                          "title": "  토요일 성수 데이트  ",
                          "memo": "  3시 전엔 출발  ",
                          "plannedDate": "2026-09-20",
                          "places": [
                            { "placeId": 2, "orderNo": 1, "memo": "   " },
                            { "placeId": 1, "orderNo": 0, "memo": "웨이팅 있으면 옆집" }
                          ]
                        }
                        """.trimIndent(),
                    ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.planId").value(PLAN_ID))

            val plan = jdbcTemplate.queryForMap("SELECT user_id, title, memo, source_course_id FROM plans WHERE id = 1")
            assertEquals(USER_ID, plan["user_id"])
            assertEquals("토요일 성수 데이트", plan["title"]) // 제목·메모는 도메인이 트림한다
            assertEquals("3시 전엔 출발", plan["memo"])
            assertNull(plan["source_course_id"])
            assertEquals("2026-09-20", dateText("SELECT planned_date::text FROM plans WHERE id = 1"))

            // 장소는 orderNo 오름차순으로 저장되고, 공백뿐인 메모는 null 로 정규화된다.
            val places =
                jdbcTemplate.queryForList(
                    "SELECT place_id, order_no, memo FROM plan_places WHERE plan_id = 1 ORDER BY order_no",
                )
            assertEquals(listOf(1L, 2L), places.map { it["place_id"] })
            assertEquals(listOf(0, 1), places.map { it["order_no"] })
            assertEquals("웨이팅 있으면 옆집", places[0]["memo"])
            assertNull(places[1]["memo"])
        }

        @Test
        fun `제목·메모·날짜 없이 장소 한 곳만으로도 만들 수 있다`() {
            mockMvc
                .perform(createRequest("""{"places":[{"placeId":1,"orderNo":0}]}"""))
                .andExpect(status().isCreated)

            val plan = jdbcTemplate.queryForMap("SELECT title, memo, planned_date FROM plans WHERE id = 1")
            assertEquals("", plan["title"]) // plans.title 은 NOT NULL 이라 빈 문자열로 저장한다
            assertNull(plan["memo"])
            assertNull(plan["planned_date"])
        }

        @Test
        fun `원본 코스를 지정하면 그 id 를 기록한다`() {
            mockMvc
                .perform(createRequest("""{"sourceCourseId":901,"places":[{"placeId":1,"orderNo":0}]}"""))
                .andExpect(status().isCreated)

            assertEquals(
                SOURCE_COURSE_ID,
                jdbcTemplate.queryForObject("SELECT source_course_id FROM plans WHERE id = 1", Long::class.java),
            )
        }

        @Test
        fun `없는 원본 코스를 지정하면 4041 이고 계획을 남기지 않는다`() {
            mockMvc
                .perform(createRequest("""{"sourceCourseId":999999,"places":[{"placeId":1,"orderNo":0}]}"""))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))

            assertEquals(0, countPlans())
        }

        @Test
        fun `없는 장소를 담으면 4043 이고 계획을 남기지 않는다`() {
            mockMvc
                .perform(createRequest("""{"places":[{"placeId":1,"orderNo":0},{"placeId":999999,"orderNo":1}]}"""))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4043))

            assertEquals(0, countPlans())
        }

        @Test
        fun `장소를 한 곳도 담지 않으면 도메인이 4001 로 막는다`() {
            mockMvc
                .perform(createRequest("""{"title":"빈 계획","places":[]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, countPlans())
        }

        @Test
        fun `orderNo 가 중복되면 도메인이 4001 로 막는다`() {
            mockMvc
                .perform(createRequest("""{"places":[{"placeId":1,"orderNo":0},{"placeId":2,"orderNo":0}]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, countPlans())
        }

        @Test
        fun `장소가 10곳을 넘으면 요청 검증이 4002 로 막는다`() {
            val places = (0..10).joinToString(",") { """{"placeId":1,"orderNo":$it}""" }

            mockMvc
                .perform(createRequest("""{"places":[$places]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("places"))
        }

        /**
         * plan_places.order_no 는 SMALLINT 라 저장 시 toShort() 로 잘린다 —
         * 상한이 없으면 32768 이 −32768 로 뒤집혀 순서가 조용히 바뀌므로 요청 단계에서 막는다.
         */
        @Test
        fun `orderNo 가 SMALLINT 범위를 넘으면 4002 로 막는다`() {
            mockMvc
                .perform(createRequest("""{"places":[{"placeId":1,"orderNo":32768}]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("places[0].orderNo"))

            assertEquals(0, countPlans())
        }

        @Test
        fun `메모가 500자를 넘으면 4002 로 막는다`() {
            val tooLong = "a".repeat(501)

            mockMvc
                .perform(createRequest("""{"memo":"$tooLong","places":[{"placeId":1,"orderNo":0}]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))

            mockMvc
                .perform(createRequest("""{"places":[{"placeId":1,"orderNo":0,"memo":"$tooLong"}]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `제목이 200자를 넘으면 4002 로 막는다`() {
            mockMvc
                .perform(createRequest("""{"title":"${"가".repeat(201)}","places":[{"placeId":1,"orderNo":0}]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
        }

        @Test
        fun `토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"places":[{"placeId":1,"orderNo":0}]}"""),
                ).andExpect(status().isUnauthorized)

            mockMvc.perform(get("/api/v1/plans")).andExpect(status().isUnauthorized)
        }

        /** 회원가입 토큰에는 subject(userId)가 없어 `@CurrentUserId` 해석에서 먼저 막힌다 — 403 이 아니라 401 이다. */
        @Test
        fun `회원가입 토큰으로는 계획을 만들 수 없다`() {
            val registrationToken = jwtTokenProvider.issueRegistrationToken(SocialProvider.KAKAO, "social-1")

            mockMvc
                .perform(
                    post("/api/v1/plans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $registrationToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"places":[{"placeId":1,"orderNo":0}]}"""),
                ).andExpect(status().isUnauthorized)

            assertEquals(0, countPlans())
        }

        @Test
        fun `mock=true 면 DB 를 건드리지 않고 목 planId 를 내려준다`() {
            mockMvc
                .perform(createRequest("""{"places":[{"placeId":1,"orderNo":0}]}""").param("mock", "true"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.planId").value(1))

            assertEquals(0, countPlans())

            mockMvc
                .perform(getRequest(PLAN_ID, accessToken(USER_ID)).param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.sourceCourseId").value(12))
        }

        @Test
        fun `상세는 담은 장소를 orderNo 순으로 내려준다`() {
            createPlan(
                """{"title":"내 계획","memo":"메모","places":[{"placeId":2,"orderNo":1},{"placeId":1,"orderNo":0,"memo":"첫 장소"}]}""",
            )

            mockMvc
                .perform(getRequest(PLAN_ID, accessToken(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.planId").value(PLAN_ID))
                .andExpect(jsonPath("$.data.title").value("내 계획"))
                .andExpect(jsonPath("$.data.memo").value("메모"))
                .andExpect(jsonPath("$.data.plannedDate").doesNotExist())
                .andExpect(jsonPath("$.data.places.length()").value(2))
                .andExpect(jsonPath("$.data.places[0].placeId").value(1))
                .andExpect(jsonPath("$.data.places[0].orderNo").value(0))
                .andExpect(jsonPath("$.data.places[0].memo").value("첫 장소"))
                .andExpect(jsonPath("$.data.places[1].placeId").value(2))
                .andExpect(jsonPath("$.data.places[1].memo").doesNotExist())
        }

        @Test
        fun `없는 계획과 타인 계획은 같은 4047 로 은닉한다`() {
            createPlan()

            mockMvc
                .perform(getRequest(99999L, accessToken(USER_ID)))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))

            mockMvc
                .perform(getRequest(PLAN_ID, accessToken(OTHER_USER_ID)))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))
        }

        @Test
        fun `편집은 장소를 통째로 교체하고 원본 코스는 유지한다`() {
            createPlan(
                """{"title":"원래 제목","sourceCourseId":901,"places":[{"placeId":1,"orderNo":0},{"placeId":2,"orderNo":1}]}""",
            )
            val createdAt = timestampText("SELECT created_at::text FROM plans WHERE id = 1")
            val updatedAt = timestampText("SELECT updated_at::text FROM plans WHERE id = 1")

            mockMvc
                .perform(
                    patchRequest(
                        PLAN_ID,
                        accessToken(USER_ID),
                        """
                        {
                          "title": "수정된 제목",
                          "memo": "2시 반 출발로 변경",
                          "plannedDate": "2026-09-21",
                          "places": [ { "placeId": 3, "orderNo": 0, "memo": "먼저" } ]
                        }
                        """.trimIndent(),
                    ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.planId").value(PLAN_ID))

            val plan = jdbcTemplate.queryForMap("SELECT title, memo, source_course_id FROM plans WHERE id = 1")
            assertEquals("수정된 제목", plan["title"])
            assertEquals("2시 반 출발로 변경", plan["memo"])
            assertEquals(SOURCE_COURSE_ID, plan["source_course_id"]) // 요청에 없어도 저장된 원본이 유지된다
            assertEquals("2026-09-21", dateText("SELECT planned_date::text FROM plans WHERE id = 1"))

            // 기존 장소 2곳이 사라지고 보낸 1곳만 남는다(전체 치환).
            val places =
                jdbcTemplate.queryForList(
                    "SELECT place_id, order_no, memo FROM plan_places WHERE plan_id = 1 ORDER BY order_no",
                )
            assertEquals(1, places.size)
            assertEquals(3L, places[0]["place_id"])
            assertEquals("먼저", places[0]["memo"])

            // created_at 은 그대로, updated_at 만 새로 찍힌다.
            assertEquals(createdAt, timestampText("SELECT created_at::text FROM plans WHERE id = 1"))
            assertNotEquals(updatedAt, timestampText("SELECT updated_at::text FROM plans WHERE id = 1"))
        }

        @Test
        fun `타인 계획은 편집할 수 없다`() {
            createPlan()

            mockMvc
                .perform(
                    patchRequest(
                        PLAN_ID,
                        accessToken(OTHER_USER_ID),
                        """{"title":"뺏기","places":[{"placeId":1,"orderNo":0}]}""",
                    ),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))

            assertEquals(
                "원래 제목",
                jdbcTemplate.queryForObject("SELECT title FROM plans WHERE id = 1", String::class.java),
            )
        }

        @Test
        fun `삭제하면 소프트 삭제되고 장소는 남지만 조회되지 않는다`() {
            createPlan()

            mockMvc
                .perform(deleteRequest(PLAN_ID, accessToken(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("계획이 삭제되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())

            // 행은 남고 스탬프만 찍힌다 — 자식(plan_places)은 지우지 않는다.
            assertEquals(1, countRows("SELECT count(*) FROM plans WHERE id = 1 AND deleted_at IS NOT NULL"))
            assertEquals(2, countRows("SELECT count(*) FROM plan_places WHERE plan_id = 1"))

            // 이미 지운 계획은 상세·재삭제·편집 모두 같은 4047 이다(멱등).
            mockMvc.perform(getRequest(PLAN_ID, accessToken(USER_ID))).andExpect(jsonPath("$.code").value(4047))
            mockMvc.perform(deleteRequest(PLAN_ID, accessToken(USER_ID))).andExpect(jsonPath("$.code").value(4047))
            mockMvc
                .perform(patchRequest(PLAN_ID, accessToken(USER_ID), """{"places":[{"placeId":1,"orderNo":0}]}"""))
                .andExpect(jsonPath("$.code").value(4047))
        }

        @Test
        fun `타인 계획은 삭제할 수 없다`() {
            createPlan()

            mockMvc
                .perform(deleteRequest(PLAN_ID, accessToken(OTHER_USER_ID)))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))

            assertEquals(1, countRows("SELECT count(*) FROM plans WHERE id = 1 AND deleted_at IS NULL"))
        }

        @Test
        fun `mock=true 삭제는 DB 를 건드리지 않는다`() {
            createPlan()

            mockMvc
                .perform(deleteRequest(PLAN_ID, accessToken(USER_ID)).param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("계획이 삭제되었습니다."))

            assertEquals(1, countRows("SELECT count(*) FROM plans WHERE id = 1 AND deleted_at IS NULL"))
        }

        @Sql(scripts = ["/sql/plan-list-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Test
        fun `목록은 최근 수정순이고 삭제·타인 계획을 빼고 장소 수를 함께 준다`() {
            mockMvc
                .perform(listRequest(accessToken(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.plans.length()").value(3))
                .andExpect(jsonPath("$.data.plans[0].planId").value(12))
                .andExpect(jsonPath("$.data.plans[0].placeCount").value(0))
                .andExpect(jsonPath("$.data.plans[1].planId").value(11))
                .andExpect(jsonPath("$.data.plans[1].placeCount").value(1))
                .andExpect(jsonPath("$.data.plans[1].plannedDate").value("2026-09-20"))
                .andExpect(jsonPath("$.data.plans[2].planId").value(10))
                .andExpect(jsonPath("$.data.plans[2].placeCount").value(2))
        }

        @Sql(scripts = ["/sql/plan-list-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Test
        fun `커서로 다음 페이지를 이어 읽는다`() {
            val firstPage =
                mockMvc
                    .perform(listRequest(accessToken(USER_ID)).param("size", "2"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.plans[0].planId").value(12))
                    .andExpect(jsonPath("$.data.plans[1].planId").value(11))
                    .andReturn()
                    .response.contentAsString
            val cursor = Regex("\"nextCursor\":\"([^\"]+)\"").find(firstPage)!!.groupValues[1]

            mockMvc
                .perform(listRequest(accessToken(USER_ID)).param("size", "2").param("cursor", cursor))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.plans.length()").value(1))
                .andExpect(jsonPath("$.data.plans[0].planId").value(10))
        }

        @Test
        fun `목록도 mock=true 면 DB 조회 없이 고정 목을 내려준다`() {
            mockMvc
                .perform(listRequest(accessToken(USER_ID)).param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.plans.length()").value(2))
                .andExpect(jsonPath("$.data.plans[0].planId").value(1))
                .andExpect(jsonPath("$.data.plans[0].placeCount").value(2))
        }

        @Test
        fun `계획이 없으면 빈 목록이다`() {
            mockMvc
                .perform(listRequest(accessToken(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.plans.length()").value(0))
        }

        @Test
        fun `잘못된 커서는 4001, size 범위 밖은 4002 다`() {
            mockMvc
                .perform(listRequest(accessToken(USER_ID)).param("cursor", "broken"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            mockMvc
                .perform(listRequest(accessToken(USER_ID)).param("size", "51"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))

            mockMvc
                .perform(listRequest(accessToken(USER_ID)).param("size", "0"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        /** 기본 픽스처 위에 계획 1건(id=1)을 만들어 둔다 — 상세·편집·삭제 테스트의 사전 상태. */
        private fun createPlan(
            body: String = """{"title":"원래 제목","places":[{"placeId":1,"orderNo":0},{"placeId":2,"orderNo":1}]}""",
        ) {
            mockMvc.perform(createRequest(body)).andExpect(status().isCreated)
        }

        private fun createRequest(body: String) =
            post("/api/v1/plans")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken(USER_ID)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)

        private fun getRequest(
            planId: Long,
            token: String,
        ) = get("/api/v1/plans/$planId").header(HttpHeaders.AUTHORIZATION, "Bearer $token")

        private fun listRequest(token: String) = get("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, "Bearer $token")

        private fun patchRequest(
            planId: Long,
            token: String,
            body: String,
        ) = patch("/api/v1/plans/$planId")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)

        private fun deleteRequest(
            planId: Long,
            token: String,
        ) = delete("/api/v1/plans/$planId").header(HttpHeaders.AUTHORIZATION, "Bearer $token")

        private fun accessToken(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private fun countPlans(): Int = countRows("SELECT count(*) FROM plans")

        private fun countRows(sql: String): Int = jdbcTemplate.queryForObject(sql, Int::class.java) ?: 0

        private fun dateText(sql: String): String? = jdbcTemplate.queryForObject(sql, String::class.java)

        private fun timestampText(sql: String): String? = jdbcTemplate.queryForObject(sql, String::class.java)

        private companion object {
            const val PLAN_ID = 1L
            const val USER_ID = 1L
            const val OTHER_USER_ID = 2L
            const val SOURCE_COURSE_ID = 901L
        }
    }
