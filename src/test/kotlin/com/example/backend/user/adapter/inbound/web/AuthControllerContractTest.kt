package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.exception.GlobalExceptionHandler
import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.inbound.AuthUseCase
import com.example.backend.user.application.port.inbound.dto.LoginResult
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import com.example.backend.user.domain.model.SocialProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerContractTest {
    private val auth = mock(AuthUseCase::class.java)
    private val controller = AuthController(auth, MockGuard(MockEnvironment().apply { setActiveProfiles("prod") }))
    private val mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(GlobalExceptionHandler()).build()

    @Test
    fun `네이버 accessToken 을 유스케이스에 전달하고 신규 회원 응답을 유지한다`() {
        `when`(auth.socialLogin(SocialProvider.NAVER, "naver-token"))
            .thenReturn(LoginResult(isNewUser = true, registrationToken = "registration-token"))

        mvc
            .perform(
                post("/api/v1/auth/social-login?mock=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"provider":"NAVER","accessToken":"naver-token"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isNewUser").value(true))
            .andExpect(jsonPath("$.data.registrationToken").value("registration-token"))
        verify(auth).socialLogin(SocialProvider.NAVER, "naver-token")
        verify(auth, never()).issueDevAccessToken()
    }

    @Test
    fun `기존 카카오 idToken 요청과 로그인 응답을 유지한다`() {
        `when`(auth.socialLogin(SocialProvider.KAKAO, "kakao-token"))
            .thenReturn(
                LoginResult(isNewUser = false, accessToken = "service-access", refreshToken = "service-refresh"),
            )

        mvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"provider":"KAKAO","idToken":"kakao-token"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value("service-access"))
            .andExpect(jsonPath("$.data.refreshToken").value("service-refresh"))
        verify(auth).socialLogin(SocialProvider.KAKAO, "kakao-token")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{\"provider\":\"NAVER\"}",
            "{\"provider\":\"NAVER\",\"idToken\":\"wrong\"}",
            "{\"provider\":\"NAVER\",\"accessToken\":\" \"}",
            "{\"provider\":\"NAVER\",\"accessToken\":\"a\",\"idToken\":\"b\"}",
            "{\"provider\":\"KAKAO\"}",
            "{\"provider\":\"KAKAO\",\"accessToken\":\"wrong\"}",
            "{\"provider\":\"KAKAO\",\"idToken\":\" \"}",
            "{\"provider\":\"KAKAO\",\"idToken\":\"a\",\"accessToken\":\"b\"}",
        ],
    )
    fun `잘못된 제공자별 토큰 요청은 인증 호출 전에 400으로 거절한다`(body: String) {
        mvc
            .perform(post("/api/v1/auth/social-login?mock=true").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(4001))
        verifyNoInteractions(auth)
    }

    @Test
    fun `운영 mock 로그인도 실제 인증 실패를 반환한다`() {
        `when`(auth.socialLogin(SocialProvider.NAVER, "invalid"))
            .thenThrow(BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED))

        mvc
            .perform(
                post("/api/v1/auth/social-login?mock=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"provider":"NAVER","accessToken":"invalid"}"""),
            ).andExpect(status().isUnauthorized)
        verify(auth, never()).issueDevAccessToken()
    }

    @Test
    fun `운영 mock 회원가입은 등록 토큰 검증을 우회하지 않는다`() {
        `when`(auth.signup(SignupCommand("invalid", "닉네임", "handle", null)))
            .thenThrow(BusinessException(CommonErrorCode.INVALID_REGISTRATION_TOKEN))

        mvc
            .perform(
                post("/api/v1/auth/signup?mock=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"registrationToken":"invalid","nickname":"닉네임","handle":"handle"}"""),
            ).andExpect(status().isUnauthorized)
        verify(auth, never()).issueDevAccessToken()
    }

    @Test
    fun `운영 mock 토큰 갱신은 실제 토큰 검증을 호출한다`() {
        `when`(auth.reissue("invalid")).thenThrow(BusinessException(CommonErrorCode.INVALID_REFRESH_TOKEN))

        mvc
            .perform(
                post("/api/v1/auth/refresh?mock=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"invalid"}"""),
            ).andExpect(status().isUnauthorized)
        verify(auth, never()).issueDevAccessToken()
    }

    @Test
    fun `운영 mock 로그아웃도 실제 토큰 폐기를 호출한다`() {
        mvc
            .perform(
                post("/api/v1/auth/logout?mock=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"refresh-token"}"""),
            ).andExpect(status().isOk)
        verify(auth).logout("refresh-token")
    }
}
