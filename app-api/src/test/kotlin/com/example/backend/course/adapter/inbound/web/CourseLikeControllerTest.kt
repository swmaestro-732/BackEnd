package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 코스 좋아요 컨트롤러(`POST`·`DELETE /api/v1/courses/{courseId}/likes`) 통합 테스트. SavedCourseControllerTest 미러.
 * 픽스처(course-like-fixture.sql): 사용자 1(주체)·2(타인), 코스1(미좋아요)·2(이미 좋아요, likes_cnt 1)·3(소프트 삭제),
 * 좋아요 주체(1)는 코스2 를 이미 좋아요한 상태. 주체 식별은 JWT(subject=userId).
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-like-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseLikeControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        // ─────────────────────────── 좋아요 (POST) ───────────────────────────

        @Test
        fun `코스를 좋아요하면 201과 liked=true·증가한 likesCnt 를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/$UNLIKED_COURSE_ID/likes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courseId").value(UNLIKED_COURSE_ID))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likesCnt").value(1)) // 픽스처 0 → 1
        }

        @Test
        fun `이미 좋아요한 코스를 다시 좋아요하면 4098을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/$LIKED_COURSE_ID/likes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4098))
        }

        @Test
        fun `없는 코스를 좋아요하면 4041을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/99999/likes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `소프트 삭제된 코스를 좋아요하면 4041을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/$DELETED_COURSE_ID/likes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `좋아요는 토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(post("/api/v1/courses/$UNLIKED_COURSE_ID/likes"))
                .andExpect(status().isUnauthorized)
        }

        // ─────────────────────────── 좋아요 취소 (DELETE) ───────────────────────────

        @Test
        fun `좋아요한 코스를 취소하면 200과 liked=false·감소한 likesCnt 를 내려준다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/$LIKED_COURSE_ID/likes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likesCnt").value(0)) // 픽스처 1 → 0
        }

        @Test
        fun `좋아요하지 않은 코스를 취소해도 200을 내려준다 (멱등)`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/$UNLIKED_COURSE_ID/likes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likesCnt").value(0)) // 취소할 게 없어 그대로 0
        }

        @Test
        fun `좋아요 취소는 토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(delete("/api/v1/courses/$LIKED_COURSE_ID/likes"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `좋아요한 코스를 상세 조회하면 viewer_hasLiked 가 true 다`() {
            mockMvc
                .perform(
                    get("/api/v1/courses/$LIKED_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.course.viewer.hasLiked").value(true))
                .andExpect(jsonPath("$.data.course.viewer.hasSaved").value(false))
        }

        @Test
        fun `취소한 좋아요를 다시 좋아요하면 201과 liked=true 를 내려준다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/$LIKED_COURSE_ID/likes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)

            // 취소가 하드 삭제라 같은 코스를 다시 좋아요할 수 있다(유니크 위반 없음).
            mockMvc
                .perform(
                    post("/api/v1/courses/$LIKED_COURSE_ID/likes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likesCnt").value(1)) // 0 → 1
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val USER_ID = 1L
            const val UNLIKED_COURSE_ID = 1L
            const val LIKED_COURSE_ID = 2L
            const val DELETED_COURSE_ID = 3L
        }
    }
