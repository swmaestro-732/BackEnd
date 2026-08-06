package com.example.backend.mobile.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 마이페이지 공개범위별 코스 카운트와 10개 고정 커서 페이지의 통합 테스트. */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/my-page-screen-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MyPageScreenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `본인 페이지는 공개범위별 발행 코스 개수를 모두 내려준다`() {
            mockMvc
                .perform(get(MY_PAGE_PATH).header(HttpHeaders.AUTHORIZATION, bearer(OWNER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.profile.publicCoursesCnt").value(10))
                .andExpect(jsonPath("$.data.profile.followerCoursesCnt").value(1))
                .andExpect(jsonPath("$.data.profile.privateCoursesCnt").value(1))
                .andExpect(jsonPath("$.data.profile.coursesCnt").doesNotExist())
        }

        @Test
        fun `본인 코스 목록을 10개씩 커서로 이어 조회하며 중복 없이 마지막 페이지를 표시한다`() {
            val first =
                mockMvc
                    .perform(get(MY_PAGE_PATH).header(HttpHeaders.AUTHORIZATION, bearer(OWNER_ID)))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.data.courses.length()").value(10))
                    .andExpect(jsonPath("$.data.courses[0].id").value("1"))
                    .andExpect(jsonPath("$.data.courses[9].id").value("11"))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").isNotEmpty)
                    .andReturn()

            val firstBody = first.response.contentAsString
            val cursor: String = JsonPath.read(firstBody, "$.data.nextCursor")
            val firstIds: List<String> = JsonPath.read(firstBody, "$.data.courses[*].id")

            val second =
                mockMvc
                    .perform(
                        get(MY_PAGE_PATH)
                            .param("cursor", cursor)
                            .header(HttpHeaders.AUTHORIZATION, bearer(OWNER_ID)),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.data.courses.length()").value(2))
                    .andExpect(jsonPath("$.data.courses[0].id").value("10"))
                    .andExpect(jsonPath("$.data.courses[1].id").value("12"))
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.nextCursor").isEmpty)
                    .andReturn()

            val secondIds: List<String> = JsonPath.read(second.response.contentAsString, "$.data.courses[*].id")
            assertTrue(firstIds.toSet().intersect(secondIds.toSet()).isEmpty())
        }

        @Test
        fun `size로 코스 목록 페이지 크기를 제한한다`() {
            mockMvc
                .perform(get(MY_PAGE_PATH).param("size", "5").header(HttpHeaders.AUTHORIZATION, bearer(OWNER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.courses.length()").value(5))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty)
        }

        @Test
        fun `size가 범위를 벗어나면 4002를 내려준다`() {
            mockMvc
                .perform(get(MY_PAGE_PATH).param("size", "0").header(HttpHeaders.AUTHORIZATION, bearer(OWNER_ID)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `비팔로워가 타인 페이지를 보면 팔로워와 비공개 코스 개수를 마스킹한다`() {
            mockMvc
                .perform(
                    get("$MY_PAGE_PATH/owner_handle")
                        .header(HttpHeaders.AUTHORIZATION, bearer(OUTSIDER_ID)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.profile.publicCoursesCnt").value(10))
                .andExpect(jsonPath("$.data.profile.followerCoursesCnt").value(0))
                .andExpect(jsonPath("$.data.profile.privateCoursesCnt").value(0))
                .andExpect(jsonPath("$.data.courses.length()").value(10))
                .andExpect(jsonPath("$.data.hasNext").value(false))
        }

        @Test
        fun `조회자가 대상을 팔로우하면 타인 페이지에 팔로워 공개 코스 개수를 노출한다`() {
            // 조회자4 → 작성자1 팔로우(isFollowing=true)일 때만 팔로워 공개 카운트가 보인다.
            mockMvc
                .perform(
                    get("$MY_PAGE_PATH/owner_handle")
                        .header(HttpHeaders.AUTHORIZATION, bearer(FOLLOWER_ID)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.profile.isFollowing").value(true))
                .andExpect(jsonPath("$.data.profile.publicCoursesCnt").value(10))
                .andExpect(jsonPath("$.data.profile.followerCoursesCnt").value(1))
                .andExpect(jsonPath("$.data.profile.privateCoursesCnt").value(0))
        }

        @Test
        fun `대상만 조회자를 팔로우하는 역방향 관계에서는 팔로워 공개 코스 개수를 마스킹한다`() {
            // 작성자1 → 조회자3(isFollower=true, isFollowing=false)은 노출 조건이 아니다 → 0으로 마스킹.
            mockMvc
                .perform(
                    get("$MY_PAGE_PATH/owner_handle")
                        .header(HttpHeaders.AUTHORIZATION, bearer(REVERSE_FOLLOWER_ID)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.profile.isFollower").value(true))
                .andExpect(jsonPath("$.data.profile.isFollowing").value(false))
                .andExpect(jsonPath("$.data.profile.followerCoursesCnt").value(0))
                .andExpect(jsonPath("$.data.profile.privateCoursesCnt").value(0))
        }

        @Test
        fun `잘못된 코스 커서는 4001을 내려준다`() {
            mockMvc
                .perform(
                    get(MY_PAGE_PATH)
                        .param("cursor", "%%%")
                        .header(HttpHeaders.AUTHORIZATION, bearer(OWNER_ID)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `mock 응답도 세 카운트와 종료된 페이지 메타를 내려준다`() {
            mockMvc
                .perform(get("$MY_PAGE_PATH?mock=true").header(HttpHeaders.AUTHORIZATION, bearer(OWNER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.profile.publicCoursesCnt").value(12))
                .andExpect(jsonPath("$.data.profile.followerCoursesCnt").value(3))
                .andExpect(jsonPath("$.data.profile.privateCoursesCnt").value(2))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty)
                .andExpect(jsonPath("$.data.hasNext").value(false))
        }

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private companion object {
            const val MY_PAGE_PATH = "/service/v1/mypage"
            const val OWNER_ID = 1L
            const val OUTSIDER_ID = 2L
            const val REVERSE_FOLLOWER_ID = 3L
            const val FOLLOWER_ID = 4L
        }
    }
