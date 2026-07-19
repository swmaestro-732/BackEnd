package com.example.backend.user.adapter.inbound.web.request

import com.example.backend.user.domain.model.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 소셜 로그인 제공자. 요청은 Enum 으로 받는다(api-design 데이터 타입 정책). */
enum class SocialProvider {
    KAKAO,
    APPLE,
    GOOGLE,
}

/** 모킹 요청 DTO — 소셜 로그인. */
data class SocialLoginRequest(
    val provider: SocialProvider,
    @field:NotBlank
    val idToken: String,
)

/** 모킹 요청 DTO — 회원가입(프로필 설정). 온보딩의 관심 지역·태그는 선택. */
data class SignupRequest(
    @field:NotBlank
    @field:Size(max = User.MAX_NICKNAME_LENGTH)
    val nickname: String,
    @field:NotBlank
    val handle: String,
    val profileImageUrl: String? = null,
    val areaCodes: List<String>? = null,
    val likeTagIds: List<Long>? = null,
)

/** 모킹 요청 DTO — accessToken 재발급. */
data class TokenReissueRequest(
    @field:NotBlank
    val refreshToken: String,
)
