package com.example.backend.user.adapter.outbound.social

import com.example.backend.bootstrap.security.AppleOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.domain.model.SocialProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

/** Apple identityToken(OIDC)을 JWK 기반 decoder 로 검증한다. socialId = Apple 계정 sub(팀 단위 안정 식별자). */
@Component
class AppleSocialVerifier(
    @param:Qualifier("appleJwtDecoder")
    private val appleJwtDecoder: JwtDecoder,
    private val appleOauthProperties: AppleOauthProperties,
) : SocialTokenVerifier {
    override val provider: SocialProvider = SocialProvider.APPLE

    override fun verify(idToken: String): SocialIdentity {
        // Bundle ID·Services ID 중 하나라도 설정돼 있으면 진행. 전부 비면 Apple 로그인 미설정.
        val configured =
            listOf(appleOauthProperties.clientId, appleOauthProperties.serviceId).any { it.isNotBlank() }
        if (!configured) {
            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
        val jwt =
            try {
                appleJwtDecoder.decode(idToken)
            } catch (exception: JwtException) {
                throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
            }
        val socialId =
            jwt.subject?.takeIf(String::isNotBlank)
                ?: throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)

        return SocialIdentity(provider = SocialProvider.APPLE, socialId = socialId)
    }
}
