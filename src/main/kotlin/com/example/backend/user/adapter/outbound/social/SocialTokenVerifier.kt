package com.example.backend.user.adapter.outbound.social

import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.domain.model.SocialProvider

/** provider 별 소셜 ID 토큰 검증 전략. 디스패처(SocialVerificationAdapter)가 provider 로 라우팅한다. */
interface SocialTokenVerifier {
    val provider: SocialProvider

    fun verify(idToken: String): SocialIdentity
}
