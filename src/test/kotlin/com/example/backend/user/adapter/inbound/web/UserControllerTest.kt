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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "TRUNCATE TABLE follows, refresh_tokens, users RESTART IDENTITY CASCADE",
            "INSERT INTO users (nickname, handle) VALUES ('나테스트', 'me_handle')",
            "INSERT INTO users (nickname, handle) VALUES ('상대테스트', 'target_handle')",
            "INSERT INTO follows (follower_id, following_id) VALUES (1, 2)",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class UserControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        @Sql(
            statements = ["TRUNCATE TABLE follows, refresh_tokens, users RESTART IDENTITY CASCADE"],
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        )
        fun `유저 생성 후 목록에 포함된다`() {
            mockMvc
                .post("/api/v1/users", """{"nickname":"hello"}""")
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.nickname").value("hello"))

            mockMvc
                .perform(get("/api/v1/users"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data[0].nickname").value("hello"))
        }

        @Test
        @Sql(
            statements = ["TRUNCATE TABLE follows, refresh_tokens, users RESTART IDENTITY CASCADE"],
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        )
        fun `닉네임이 비면 400과 에러 응답`() {
            mockMvc
                .post("/api/v1/users", """{"nickname":""}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
        }

        @Test
        fun `팔로우 중인 사용자의 프로필을 조회하면 팔로우 상태를 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/users/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.isFollowing").value(true))
        }

        @Test
        fun `없는 사용자의 프로필을 조회하면 4042를 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/users/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(1)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4042))
        }

        @Test
        fun `비로그인 사용자는 프로필을 조회하면 팔로우 상태가 거짓이다`() {
            mockMvc
                .perform(get("/api/v1/users/2"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.isFollowing").value(false))
        }

        private fun MockMvc.post(
            url: String,
            body: String,
        ) = perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body),
        )

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)
    }
