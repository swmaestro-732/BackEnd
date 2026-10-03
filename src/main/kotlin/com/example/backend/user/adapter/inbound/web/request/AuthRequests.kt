package com.example.backend.user.adapter.inbound.web.request

import com.example.backend.user.domain.model.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import com.example.backend.user.domain.model.SocialProvider as DomainSocialProvider

/** 소셜 로그인 제공자. 요청은 Enum 으로 받는다(api-design 데이터 타입 정책). */
enum class SocialProvider {
    KAKAO,
    NAVER,
    APPLE,
    GOOGLE,
    ;

    fun toDomain(): DomainSocialProvider = DomainSocialProvider.valueOf(name)
}

/** 소셜 로그인 요청 DTO. */
data class SocialLoginRequest(
    val provider: SocialProvider,
    val idToken: String? = null,
    val accessToken: String? = null,
) {
    /** 기존 ID 토큰 계약을 유지하면서 네이버만 액세스 토큰으로 인증한다. */
    fun resolveToken(): String =
        when (provider) {
            SocialProvider.NAVER -> {
                require(idToken == null && !accessToken.isNullOrBlank()) {
                    "네이버 로그인은 accessToken만 입력해야 합니다."
                }
                accessToken
            }

            else -> {
                require(accessToken == null && !idToken.isNullOrBlank()) {
                    "해당 소셜 로그인은 idToken만 입력해야 합니다."
                }
                idToken
            }
        }
}

/** 회원가입(프로필 설정) 요청 DTO. areaCodes 는 법정동코드(10자리), likeThemes 는 관심 테마(코스 카테고리 이름) 목록이다. */
data class SignupRequest(
    @field:NotBlank
    val registrationToken: String,
    @field:NotBlank
    @field:Size(max = User.MAX_NICKNAME_LENGTH)
    val nickname: String,
    @field:NotBlank
    val handle: String,
    val profileImageUrl: String? = null,
    val areaCodes: List<String>? = null,
    val likeThemes: List<String>? = null,
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
