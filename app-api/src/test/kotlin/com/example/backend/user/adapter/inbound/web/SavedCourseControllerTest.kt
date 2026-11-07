package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 코스 저장 컨트롤러(`POST /api/v1/saved-courses` · `DELETE /api/v1/saved-courses/{id}` · `GET /api/v1/my/saved-courses`) 통합 테스트.
 * 픽스처(saved-course-fixture.sql)로 저장 주체(1)·타인(2)·코스 5건·폴더 3건·기존 저장 4건을 심고,
 * 저장 주체 식별은 JWT(subject=userId)로 한다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/saved-course-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SavedCourseControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        // ─────────────────────────── 저장 (POST /api/v1/saved-courses) ───────────────────────────

        @Test
        fun `코스를 저장하면 201과 성공 코드를 내려주고 저장함에 반영된다`() {
            val body = """{"courseId": $UNSAVED_COURSE_ID, "folderId": $MY_FOLDER_ID}"""

            mockMvc
                .perform(
                    post("/api/v1/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))

            // 저장 후 저장함 개수가 4 → 5로 늘고, 방금 저장한 코스가 최상단(최신)이다.
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.savedCourses[0].courseId").value(UNSAVED_COURSE_ID))
        }

        @Test
        fun `folderId 없이 저장하면 미분류로 저장된다`() {
            val body = """{"courseId": $UNSAVED_COURSE_ID, "folderId": null}"""

            mockMvc
                .perform(
                    post("/api/v1/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))

            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.savedCourses[0].courseId").value(UNSAVED_COURSE_ID))
                .andExpect(jsonPath("$.data.savedCourses[0].folderId").doesNotExist())
        }

        @Test
        fun `없는 코스를 저장하면 4041을 내려준다`() {
            val body = """{"courseId": 99999, "folderId": null}"""

            mockMvc
                .perform(
                    post("/api/v1/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `타인 소유 폴더로 저장하면 4001을 내려준다`() {
            val body = """{"courseId": $UNSAVED_COURSE_ID, "folderId": $OTHERS_FOLDER_ID}"""

            mockMvc
                .perform(
                    post("/api/v1/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `이미 저장한 코스를 다시 저장하면 4093을 내려준다`() {
            val body = """{"courseId": $SAVED_COURSE_ID, "folderId": null}"""

            mockMvc
                .perform(
                    post("/api/v1/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4093))
        }

        @Test
        fun `courseId가 양수가 아니면 검증 실패로 4002를 내려준다`() {
            val body = """{"courseId": 0, "folderId": null}"""

            mockMvc
                .perform(
                    post("/api/v1/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `저장 mock=true면 DB 저장 없이 성공을 내려준다`() {
            val body = """{"courseId": $UNSAVED_COURSE_ID, "folderId": null}"""

            mockMvc
                .perform(
                    post("/api/v1/saved-courses")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))

            // 실제 저장은 일어나지 않아 저장함 개수는 그대로 4다.
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(jsonPath("$.data.totalCount").value(4))
        }

        // ─────────────────────────── 저장 취소 (DELETE /api/v1/saved-courses/{courseId}) ───────────────────────────

        @Test
        fun `저장한 코스를 취소하면 200을 내려주고 저장함에서 빠진다`() {
            mockMvc
                .perform(
                    delete("/api/v1/saved-courses/$SAVED_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(jsonPath("$.data.totalCount").value(3))
        }

        @Test
        fun `저장하지 않은 코스를 취소해도 200을 내려준다 (멱등)`() {
            mockMvc
                .perform(
                    delete("/api/v1/saved-courses/$UNSAVED_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
        }

        // ─────────────────────────── 구 경로 (프론트 이관 전까지 유지, 호출량 0 확인 후 제거) ───────────────────────────

        @Test
        fun `구 경로 POST courses-save 로도 저장된다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/save")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"courseId": $UNSAVED_COURSE_ID, "folderId": null}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))

            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(jsonPath("$.data.totalCount").value(5))
        }

        @Test
        fun `구 경로 DELETE courses-save 로도 저장이 취소된다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/save/$SAVED_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(jsonPath("$.data.totalCount").value(3))
        }

        @Test
        fun `취소한 코스를 다시 저장하면 새 레코드로 발행되고 소프트 삭제된 행은 남지 않는다`() {
            mockMvc
                .perform(
                    delete("/api/v1/saved-courses/$SAVED_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)

            // 재저장은 다른 폴더로 — 새 레코드가 이번 요청의 folderId 를 갖는지 함께 본다.
            mockMvc
                .perform(
                    post("/api/v1/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"courseId": $SAVED_COURSE_ID, "folderId": $MY_OTHER_FOLDER_ID}"""),
                ).andExpect(status().isCreated)

            // 픽스처 저장 4건 그대로(누적 없음)이고, 방금 재저장한 코스가 새 id 로 최상단에 온다.
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.savedCourses.length()").value(4))
                .andExpect(jsonPath("$.data.savedCourses[0].courseId").value(SAVED_COURSE_ID))
                .andExpect(jsonPath("$.data.savedCourses[0].folderId").value(MY_OTHER_FOLDER_ID))
                // 되살리기가 아니라 새 발행이라 픽스처 id(1~4)보다 큰 id 를 받는다.
                .andExpect(jsonPath("$.data.savedCourses[0].id").value(greaterThan(FIXTURE_MAX_SAVED_ID)))
        }

        // ─────────────────────────── 저장 코스 조회 (GET /api/v1/my/saved-courses) ───────────────────────────

        @Test
        fun `저장 코스를 최신 저장순으로 조회한다`() {
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.savedCourses.length()").value(4))
                // id 내림차순(최신 저장순): 4,3,2,1
                .andExpect(jsonPath("$.data.savedCourses[0].id").value(4))
                .andExpect(jsonPath("$.data.savedCourses[3].id").value(1))
        }

        @Test
        fun `folderId로 필터하면 해당 폴더의 저장 코스만 조회한다`() {
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .param("folderId", MY_FOLDER_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.savedCourses.length()").value(2))
                .andExpect(jsonPath("$.data.savedCourses[0].id").value(2))
                .andExpect(jsonPath("$.data.savedCourses[1].id").value(1))
        }

        @Test
        fun `size로 페이지를 나누고 커서로 다음 페이지를 조회한다`() {
            // 첫 페이지: 최신 2건(id 4,3), 다음 커서는 3.
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("3"))
                .andExpect(jsonPath("$.data.savedCourses.length()").value(2))
                .andExpect(jsonPath("$.data.savedCourses[0].id").value(4))
                .andExpect(jsonPath("$.data.savedCourses[1].id").value(3))

            // 다음 페이지: 커서 3 이후 나머지 2건(id 2,1), 더 없음.
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .param("size", "2")
                        .param("cursor", "3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.savedCourses[0].id").value(2))
                .andExpect(jsonPath("$.data.savedCourses[1].id").value(1))
        }

        @Test
        fun `잘못된 커서면 4001을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .param("cursor", "not-a-number")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `저장 코스 조회는 토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(get("/api/v1/my/saved-courses"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `조회 mock=true면 DB와 무관하게 목 저장함을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/my/saved-courses")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.savedCourses.length()").value(4))
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val USER_ID = 1L

            // 픽스처: 코스1 은 미저장(저장 성공용), 코스2 는 이미 저장됨(중복·취소용)
            const val UNSAVED_COURSE_ID = 1L
            const val SAVED_COURSE_ID = 2L

            // 폴더 1·2 = USER_ID 소유, 폴더 3 = 타인 소유
            const val MY_FOLDER_ID = 1L
            const val MY_OTHER_FOLDER_ID = 2L
            const val OTHERS_FOLDER_ID = 3L

            // 픽스처가 심는 저장 레코드 id 는 1~4 — 재저장이 새 id 를 받는지 비교하는 기준값
            const val FIXTURE_MAX_SAVED_ID = 4
        }
    }
