package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 팔로워/팔로잉 목록 조회(GET /api/v1/users/{userId}/followers|followings) 통합 테스트.
 *
 * 시드(모든 테스트 공통):
 * - users: 1=조회자(팔로워 3·팔로잉 2), 2·3·4=상대. 카운터(totalCount 근거)는 follows 와 일치하도록 명시한다.
 * - follows(삽입순 = follows.id): (1→2)=1, (1→3)=2, (2→1)=3, (3→1)=4, (4→1)=5.
 *   → user1 팔로워(following_id=1): id 5·4·3 → 최신순 [4,3,2]. user1 팔로잉(follower_id=1): id 2·1 → [3,2].
 */
@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "TRUNCATE TABLE follows, refresh_tokens, users RESTART IDENTITY CASCADE",
            "INSERT INTO users (nickname, handle, followers_cnt, followings_cnt) " +
                "VALUES ('조회자', 'viewer_h', 3, 2)",
            "INSERT INTO users (nickname, handle) VALUES ('상대1', 'target1_h')",
            "INSERT INTO users (nickname, handle) VALUES ('상대2', 'target2_h')",
            "INSERT INTO users (nickname, handle) VALUES ('상대3', 'target3_h')",
            "INSERT INTO follows (follower_id, following_id) VALUES (1, 2)",
            "INSERT INTO follows (follower_id, following_id) VALUES (1, 3)",
            "INSERT INTO follows (follower_id, following_id) VALUES (2, 1)",
            "INSERT INTO follows (follower_id, following_id) VALUES (3, 1)",
            "INSERT INTO follows (follower_id, following_id) VALUES (4, 1)",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class UserControllerFollowListTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `팔로워 목록은 최신 팔로우순으로 내려주고 뷰어 기준 관계 플래그를 반영한다`() {
            mockMvc
                .perform(
                    get("/api/v1/users/1/followers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                // 최신 팔로우순: 4 → 3 → 2
                .andExpect(jsonPath("$.data.users[*].id").value(contains(4, 3, 2)))
                // user4: 내가(1) 팔로우 안 함 / user4 는 나를 팔로우
                .andExpect(jsonPath("$.data.users[0].nickname").value("상대3"))
                .andExpect(jsonPath("$.data.users[0].isFollowing").value(false))
                .andExpect(jsonPath("$.data.users[0].isFollower").value(true))
                // user3: 내가 팔로우 함 / user3 도 나를 팔로우(맞팔)
                .andExpect(jsonPath("$.data.users[1].id").value(3))
                .andExpect(jsonPath("$.data.users[1].isFollowing").value(true))
                .andExpect(jsonPath("$.data.users[1].isFollower").value(true))
        }

        @Test
        fun `팔로잉 목록은 대상이 팔로우하는 사용자를 최신순으로 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/users/1/followings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                // 내가 팔로우한 사람들: 3 → 2, 둘 다 맞팔이라 isFollower=true
                .andExpect(jsonPath("$.data.users[*].id").value(contains(3, 2)))
                .andExpect(jsonPath("$.data.users[0].isFollowing").value(true))
                .andExpect(jsonPath("$.data.users[0].isFollower").value(true))
        }

        @Test
        fun `비로그인 조회는 관계 플래그를 모두 거짓으로 내려준다`() {
            mockMvc
                .perform(get("/api/v1/users/1/followers"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.users[*].id").value(contains(4, 3, 2)))
                .andExpect(jsonPath("$.data.users[1].isFollowing").value(false))
                .andExpect(jsonPath("$.data.users[1].isFollower").value(false))
        }

        @Test
        fun `size와 cursor로 커서 페이지네이션한다`() {
            // 1페이지: size=1 → [4], hasNext, nextCursor=마지막 follows.id(5)
            mockMvc
                .perform(
                    get("/api/v1/users/1/followers")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.users.length()").value(1))
                .andExpect(jsonPath("$.data.users[0].id").value(4))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("5"))

            // 2페이지: cursor=5 → [3], nextCursor=4
            mockMvc
                .perform(
                    get("/api/v1/users/1/followers")
                        .param("size", "1")
                        .param("cursor", "5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.users[0].id").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("4"))
        }

        @Test
        fun `mock=true면 DB와 무관하게 목 목록을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/users/999/followers")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.users.length()").value(3))
                .andExpect(jsonPath("$.data.users[0].nickname").value("성수러버"))
        }

        @Test
        fun `없는 사용자의 목록을 조회하면 4042를 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/users/999/followers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4042))
        }

        @Test
        fun `잘못된 커서는 400을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/users/1/followers")
                        .param("cursor", "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `size가 범위를 벗어나면 400을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/users/1/followers")
                        .param("size", "51")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)
    }
