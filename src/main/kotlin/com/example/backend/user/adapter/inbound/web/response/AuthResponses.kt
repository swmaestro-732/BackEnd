package com.example.backend.user.adapter.inbound.web.response

/** 모킹 응답 DTO — 소셜 로그인. isNewUser=true면 클라이언트는 회원가입(프로필 설정)으로 분기한다. */
data class SocialLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
)

/** 모킹 응답 DTO — 회원가입 완료(토큰 + 생성된 유저 요약). */
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
