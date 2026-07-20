package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "TRUNCATE TABLE follows, refresh_tokens, users RESTART IDENTITY CASCADE",
            "INSERT INTO users (nickname, handle) VALUES ('나테스트', 'me_handle')",
            "INSERT INTO users (nickname, handle) VALUES ('상대테스트', 'target_handle')",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class MyControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `내 프로필 조회는 실제 사용자의 프로필을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/my/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value(ME_ID))
                .andExpect(jsonPath("$.data.nickname").value("나테스트"))
                .andExpect(jsonPath("$.data.handle").value("me_handle"))
                .andExpect(jsonPath("$.data.followersCnt").isNumber)
                .andExpect(jsonPath("$.data.followingsCnt").isNumber)
                .andExpect(jsonPath("$.data.coursesCnt").isNumber)
        }

        @Test
        fun `내 프로필 조회는 토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(get("/api/v1/my/profile"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `내 프로필 조회 mock 폴백은 목 프로필을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/my/profile")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("현우님"))
                .andExpect(jsonPath("$.data.handle").value("@hyunwoo"))
        }

        @Test
        fun `내 프로필 조회 mockError로 4040을 주입한다`() {
            mockMvc
                .perform(
                    get("/api/v1/my/profile")
                        .param("mockError", "4040")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }

        @Test
        fun `내 프로필을 수정한다`() {
            mockMvc
                .perform(
                    patch("/api/v1/my/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"새닉네임"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
        }

        @Test
        fun `닉네임을 빈 문자열로 수정하면 필드 검증 에러를 내려준다`() {
            mockMvc
                .perform(
                    patch("/api/v1/my/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":""}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
        }

        @Test
        fun `이미 사용 중인 닉네임으로 수정하면 4091을 내려준다`() {
            mockMvc
                .perform(
                    patch("/api/v1/my/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"상대테스트"}"""),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4091))
        }

        @Test
        fun `이미 사용 중인 핸들로 수정하면 4092를 내려준다`() {
            mockMvc
                .perform(
                    patch("/api/v1/my/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"handle":"target_handle"}"""),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4092))
        }

        @Test
        fun `팔로우는 멱등이고 대상의 팔로워 수를 한 번만 늘린다`() {
            repeat(2) {
                mockMvc
                    .perform(
                        put("/api/v1/my/followings/$TARGET_ID")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.isFollowing").value(true))
                    .andExpect(jsonPath("$.data.followersCnt").value(1))
            }
        }

        @Test
        fun `팔로우한 사용자를 언팔로우하면 팔로워 수를 줄인다`() {
            mockMvc
                .perform(
                    put("/api/v1/my/followings/$TARGET_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.followersCnt").value(1))

            mockMvc
                .perform(
                    delete("/api/v1/my/followings/$TARGET_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.isFollowing").value(false))
                .andExpect(jsonPath("$.data.followersCnt").value(0))
        }

        @Test
        fun `자기 자신을 팔로우하면 4001을 내려준다`() {
            mockMvc
                .perform(
                    put("/api/v1/my/followings/$ME_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `없는 사용자를 팔로우하면 4042를 내려준다`() {
            mockMvc
                .perform(
                    put("/api/v1/my/followings/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4042))
        }

        @Test
        fun `회원 탈퇴하면 프로필 조회에서 제외된다`() {
            val accessToken = tokenFor(ME_ID)

            mockMvc
                .perform(
                    delete("/api/v1/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").doesNotExist())

            mockMvc
                .perform(
                    get("/api/v1/my/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4042))
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val ME_ID = 1L
            const val TARGET_ID = 2L
        }
    }
