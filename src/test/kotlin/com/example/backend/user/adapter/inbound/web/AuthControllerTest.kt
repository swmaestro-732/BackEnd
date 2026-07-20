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

/** 인증/세션 모킹 API(`/api/v1/auth` 하위) 검증. 목 데이터라 DB 픽스처가 필요 없다. */
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
                    post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"provider":"KAKAO","idToken":"kakao-token"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
                .andExpect(jsonPath("$.data.isNewUser").value(false))
        }

        @Test
        fun `회원가입은 토큰과 생성된 유저를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"지호","handle":"@jiho","profileImageUrl":null}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
                .andExpect(jsonPath("$.data.user.nickname").value("지호"))
        }

        @Test
        fun `회원가입 닉네임이 공백이면 4002와 fieldErrors`() {
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"","handle":"@x"}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
        }

        @Test
        fun `토큰 재발급은 새 토큰을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/auth/token-reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"refreshToken":"mock-refresh-token"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
        }

        @Test
        fun `로그아웃은 본문 없는 성공을 내려준다`() {
            mockMvc
                .perform(post("/api/v1/auth/logout"))
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
