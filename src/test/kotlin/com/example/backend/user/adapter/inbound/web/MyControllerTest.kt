package com.example.backend.user.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * User 도메인 인증·세션·프로필 모킹 API(`/api/v1/my` 하위) 검증. 목 데이터라 DB 픽스처가 필요 없다.
 */
@AutoConfigureMockMvc
class MyControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `소셜 로그인은 토큰과 신규 여부를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/my/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"provider":"KAKAO","idToken":"kakao-token"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty)
                .andExpect(jsonPath("$.data.isNewUser").value(false))
        }

        @Test
        fun `회원가입은 토큰과 생성된 유저를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/my/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"지호","handle":"@jiho","profileImageUrl":null}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
                .andExpect(jsonPath("$.data.user.nickname").value("지호"))
                .andExpect(jsonPath("$.data.user.handle").value("@jiho"))
        }

        @Test
        fun `회원가입 닉네임이 공백이면 4002와 fieldErrors`() {
            mockMvc
                .perform(
                    post("/api/v1/my/signup")
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
                    post("/api/v1/my/token-reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"refreshToken":"mock-refresh-token"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
        }

        @Test
        fun `로그아웃은 본문 없는 성공을 내려준다`() {
            mockMvc
                .perform(post("/api/v1/my/logout"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `아이디 사용 가능 여부 - 일반 값은 사용 가능`() {
            mockMvc
                .perform(get("/api/v1/my/login-id/availability").param("loginId", "hyunwoo"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.available").value(true))
        }

        @Test
        fun `아이디 사용 가능 여부 - 예약어는 사용 불가`() {
            mockMvc
                .perform(get("/api/v1/my/login-id/availability").param("loginId", "admin"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.available").value(false))
        }

        @Test
        fun `프로필 조회는 카운트 포함 프로필을 내려준다`() {
            mockMvc
                .perform(get("/api/v1/my/7"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.followersCnt").isNumber)
                .andExpect(jsonPath("$.data.coursesCnt").isNumber)
        }

        @Test
        fun `프로필 조회 mockError=4040이면 404`() {
            mockMvc
                .perform(get("/api/v1/my/7").param("mockError", "4040"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `프로필 수정은 넘어온 필드를 반영한다`() {
            mockMvc
                .perform(
                    patch("/api/v1/my/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"새닉네임"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
        }

        @Test
        fun `프로필 수정 닉네임을 빈 문자열로 보내면 4002`() {
            mockMvc
                .perform(
                    patch("/api/v1/my/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":""}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
        }

        @Test
        fun `회원 탈퇴는 본문 없는 성공을 내려준다`() {
            mockMvc
                .perform(delete("/api/v1/my"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }
