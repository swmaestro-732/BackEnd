package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.domain.model.SocialProvider
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "TRUNCATE TABLE users RESTART IDENTITY CASCADE",
            "INSERT INTO users (nickname, handle) VALUES ('인증테스트유저', 'auth_handle')",
            "INSERT INTO refresh_tokens (user_id, token_hash, expires_at) " +
                "VALUES (1, 'cac0fa7ee4d7e749b37053c07f7eae4194c38025ffb999f794dd5e7825b6019f', " +
                "now() + interval '14 days')",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class AuthTokenFlowTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `회원가입 닉네임이 이미 존재하면 4091을 내려준다`() {
            mockMvc
                .perform(signupRequest("인증테스트유저", "new_handle", "new-social-nickname"))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4091))
        }

        @Test
        fun `회원가입 핸들이 이미 존재하면 4092를 내려준다`() {
            mockMvc
                .perform(signupRequest("새인증유저", "auth_handle", "new-social-handle"))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4092))
        }

        @Test
        fun `refresh token 재발급 시 토큰을 회전하고 기존 토큰 재사용을 거부한다`() {
            mockMvc
                .perform(reissueRequest())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty)
                .andExpect(jsonPath("$.data.refreshToken").value(not(REFRESH_TOKEN)))

            mockMvc
                .perform(reissueRequest())
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value(4012))
        }

        @Test
        fun `로그아웃은 멱등이며 폐기한 refresh token 재발급을 거부한다`() {
            mockMvc
                .perform(logoutRequest())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            mockMvc
                .perform(logoutRequest())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            mockMvc
                .perform(reissueRequest())
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value(4012))
        }

        @Test
        fun `유효한 access token으로 보호된 내 프로필을 조회한다`() {
            val accessToken = jwtTokenProvider.issueAccessToken(USER_ID)

            mockMvc
                .perform(
                    get("/api/v1/my/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value(USER_ID))
        }

        private fun reissueRequest() =
            post("/api/v1/auth/token-reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$REFRESH_TOKEN"}""")

        private fun logoutRequest() =
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$REFRESH_TOKEN"}""")

        private fun signupRequest(
            nickname: String,
            handle: String,
            socialId: String,
        ): MockHttpServletRequestBuilder {
            val registrationToken = jwtTokenProvider.issueRegistrationToken(SocialProvider.KAKAO, socialId)
            return post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"registrationToken":"$registrationToken","nickname":"$nickname","handle":"$handle"}""")
        }

        private companion object {
            const val USER_ID = 1L
            const val REFRESH_TOKEN = "stage3-refresh-token"
        }
    }
