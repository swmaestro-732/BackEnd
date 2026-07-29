package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.NotBlank
import com.example.backend.user.domain.model.SocialProvider as DomainSocialProvider

/** 소셜 로그인 제공자. 요청은 Enum 으로 받는다(api-design 데이터 타입 정책). */
enum class SocialProvider {
    KAKAO,
    APPLE,
    GOOGLE,
    ;

    fun toDomain(): DomainSocialProvider = DomainSocialProvider.valueOf(name)
}

/** 소셜 로그인 요청 DTO. */
data class SocialLoginRequest(
    val provider: SocialProvider,
    @field:NotBlank
    val idToken: String,
)

/** accessToken·refreshToken 재발급 요청 DTO. */
data class TokenReissueRequest(
    @field:NotBlank
    val refreshToken: String,
)

/** 로그아웃 요청 DTO. */
data class LogoutRequest(
    @field:NotBlank
    val refreshToken: String,
)
