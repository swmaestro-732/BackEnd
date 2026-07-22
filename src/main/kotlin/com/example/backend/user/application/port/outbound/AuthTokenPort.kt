package com.example.backend.user.application.port.outbound

import com.example.backend.user.domain.model.SocialProvider

/** 서비스 자체 JWT의 발급·검증을 담당하는 아웃바운드 포트. */
interface AuthTokenPort {
    fun issueAccessToken(userId: Long): String

    fun issueRegistrationToken(
        provider: SocialProvider,
        socialId: String,
    ): String

    fun parseRegistrationToken(token: String): SocialIdentity
}
