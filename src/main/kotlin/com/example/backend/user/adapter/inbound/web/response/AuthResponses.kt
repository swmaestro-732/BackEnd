package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.LoginResult
import com.example.backend.user.application.port.inbound.dto.SignupResult
import com.fasterxml.jackson.annotation.JsonInclude

/** 소셜 로그인 응답. isNewUser=true면 registrationToken 으로 회원가입을 진행한다. */
data class SocialLoginResponse(
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val accessToken: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val refreshToken: String? = null,
    val isNewUser: Boolean,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val registrationToken: String? = null,
) {
    companion object {
        fun from(result: LoginResult): SocialLoginResponse =
            SocialLoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                isNewUser = result.isNewUser,
                registrationToken = result.registrationToken,
            )
    }
}

/** 회원가입 완료 응답(토큰 + 생성된 유저 요약). */
data class SignupResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: SignupUser,
) {
    data class SignupUser(
        val id: Long,
        val nickname: String,
        val handle: String,
        val profileImageUrl: String?,
    )

    companion object {
        fun from(result: SignupResult): SignupResponse =
            SignupResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                user =
                    SignupUser(
                        id = result.user.id,
                        nickname = result.user.nickname,
                        handle = result.user.handle,
                        profileImageUrl = result.user.profileImageUrl,
                    ),
            )
    }
}

/** 모킹 응답 DTO — accessToken 재발급. */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

/** 모킹 응답 DTO — 아이디(핸들) 사용 가능 여부. */
data class AvailabilityResponse(
    val available: Boolean,
)
