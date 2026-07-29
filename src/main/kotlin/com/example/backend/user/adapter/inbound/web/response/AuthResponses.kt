package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.LoginResult

private const val MOCK_REFRESH_TOKEN = "mock-refresh-token"

/**
 * 소셜 로그인 응답. 로그인·가입을 합쳤으므로 항상 토큰을 내려준다.
 * isNewUser=true 면 온보딩(PATCH /my/profile)에서 nickname·handle 을 설정해야 한다.
 */
data class SocialLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
) {
    companion object {
        fun from(result: LoginResult): SocialLoginResponse =
            SocialLoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                isNewUser = result.isNewUser,
            )

        fun mock(accessToken: String): SocialLoginResponse =
            SocialLoginResponse(accessToken = accessToken, refreshToken = MOCK_REFRESH_TOKEN, isNewUser = false)
    }
}

/** accessToken·refreshToken 재발급 응답 DTO. */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        fun mock(accessToken: String): TokenResponse =
            TokenResponse(accessToken = accessToken, refreshToken = MOCK_REFRESH_TOKEN)
    }
}

/** 아이디(핸들) 사용 가능 여부 응답. `GET /api/v1/users/availability` · (구) `GET /api/v1/auth/login-id/availability`. */
data class AvailabilityResponse(
    val available: Boolean,
)
