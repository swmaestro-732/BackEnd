package com.example.backend.user.adapter.outbound.social

import com.example.backend.bootstrap.security.GoogleOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.domain.model.SocialProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

/** Google ID 토큰(OIDC)을 JWK 기반 decoder 로 검증한다. socialId = Google 계정 sub. */
@Component
class GoogleSocialVerifier(
    @param:Qualifier("googleJwtDecoder")
    private val googleJwtDecoder: JwtDecoder,
    private val googleOauthProperties: GoogleOauthProperties,
) : SocialTokenVerifier {
    override val provider: SocialProvider = SocialProvider.GOOGLE

    override fun verify(idToken: String): SocialIdentity {
        if (googleOauthProperties.clientId.isBlank()) {
            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
        val jwt =
            try {
                googleJwtDecoder.decode(idToken)
            } catch (exception: JwtException) {
                throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
            }
        val socialId =
            jwt.subject?.takeIf(String::isNotBlank)
                ?: throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)

        return SocialIdentity(provider = SocialProvider.GOOGLE, socialId = socialId)
    }
}
