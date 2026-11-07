package com.example.backend.user.application.port.outbound

import com.example.backend.user.domain.model.SocialProvider

/** 검증된 소셜 계정 식별 정보. */
data class SocialIdentity(
    val provider: SocialProvider,
    val socialId: String,
)

/** 소셜 제공자가 발급한 토큰(카카오 ID 토큰·네이버 액세스 토큰)을 검증하는 아웃바운드 포트. */
interface SocialVerificationPort {
    fun verify(
        provider: SocialProvider,
        token: String,
    ): SocialIdentity
}
