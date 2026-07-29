package com.example.backend.user.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 인증/세션 mock 폴백(`/api/v1/auth` 하위) 검증. 목 데이터라 DB 픽스처가 필요 없다. */
@AutoConfigureMockMvc
class AuthControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `소셜 로그인은 토큰과 신규 여부를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/auth/social-login?mock=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"provider":"KAKAO","idToken":"kakao-token"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
                .andExpect(jsonPath("$.data.isNewUser").value(false))
        }

        @Test
        fun `요청 본문의 enum 값이 잘못되면 4001`() {
            mockMvc
                .perform(
                    post("/api/v1/auth/social-login?mock=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"provider":"NAVER","idToken":"x"}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `요청 본문이 깨진 JSON이면 4001`() {
            mockMvc
                .perform(
                    post("/api/v1/auth/social-login?mock=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"provider":"KAKAO", }"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `토큰 재발급은 새 토큰을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/auth/token-reissue?mock=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"refreshToken":"mock-refresh-token"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
        }

        @Test
        fun `로그아웃은 본문 없는 성공을 내려준다`() {
            mockMvc
                .perform(post("/api/v1/auth/logout?mock=true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `아이디 사용 가능 여부 - 일반 값은 사용 가능, 예약어는 불가`() {
            mockMvc
                .perform(get("/api/v1/auth/login-id/availability").param("loginId", "hyunwoo"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.available").value(true))
            mockMvc
                .perform(get("/api/v1/auth/login-id/availability").param("loginId", "admin"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.available").value(false))
        }

        @Test
        fun `mockError로 모킹 에러를 주입한다`() {
            mockMvc
                .perform(post("/api/v1/auth/logout").param("mockError", "4040"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }
    }
