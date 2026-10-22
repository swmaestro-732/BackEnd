package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.domain.model.SocialProvider
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 탈퇴(soft delete)한 소셜 계정의 재로그인 → 온보딩 재시작 시 회원가입이 기존 행을 재활성화하는지 검증한다.
 * (INSERT 대신 UPDATE 로 uq_users_social UNIQUE 위반 500 을 방지한다.)
 */
@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "TRUNCATE TABLE users RESTART IDENTITY CASCADE",
            "INSERT INTO users (nickname, handle, status, social_provider, social_id, deleted_at) " +
                "VALUES ('탈퇴카카오유저', 'old_kakao_handle', 'WITHDRAWN', 'KAKAO', 'withdrawn-social-id', now()), " +
                "('탈퇴네이버유저', 'old_naver_handle', 'WITHDRAWN', 'NAVER', 'withdrawn-social-id', now())",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class AuthReactivationFlowTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @ParameterizedTest
        @EnumSource(value = SocialProvider::class, names = ["KAKAO", "NAVER"])
        fun `탈퇴한 소셜 계정의 회원가입은 기존 행을 재활성화한다`(provider: SocialProvider) {
            mockMvc
                .perform(signupRequest(provider, "재가입유저", "new_handle"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty)
                // 행 재사용 — 새 INSERT 가 아니라 기존 id 를 그대로 쓴다.
                .andExpect(jsonPath("$.data.user.id").value(withdrawnUserId(provider)))
                .andExpect(jsonPath("$.data.user.nickname").value("재가입유저"))
                .andExpect(jsonPath("$.data.user.handle").value("new_handle"))
        }

        @ParameterizedTest
        @EnumSource(value = SocialProvider::class, names = ["KAKAO", "NAVER"])
        fun `탈퇴 행의 handle 을 그대로 재사용해도 성공한다(자기 자신 제외 유니크)`(provider: SocialProvider) {
            val oldHandle = "old_${provider.name.lowercase()}_handle"
            mockMvc
                .perform(signupRequest(provider, "재가입유저", oldHandle))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.user.id").value(withdrawnUserId(provider)))
                .andExpect(jsonPath("$.data.user.handle").value(oldHandle))
        }

        private fun withdrawnUserId(provider: SocialProvider): Int = if (provider == SocialProvider.KAKAO) 1 else 2

        private fun signupRequest(
            provider: SocialProvider,
            nickname: String,
            handle: String,
        ): MockHttpServletRequestBuilder {
            val registrationToken = jwtTokenProvider.issueRegistrationToken(provider, SOCIAL_ID)
            return post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"registrationToken":"$registrationToken","nickname":"$nickname","handle":"$handle"}""")
        }

        private companion object {
            const val SOCIAL_ID = "withdrawn-social-id"
        }
    }
