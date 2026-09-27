package com.example.backend.user.adapter.outbound.social

import com.example.backend.bootstrap.security.KakaoOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.domain.model.SocialProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

/** Kakao ID 토큰을 JWK 기반 decoder 로 검증한다. */
@Component
class KakaoSocialVerifier(
    @param:Qualifier("kakaoJwtDecoder")
    private val kakaoJwtDecoder: JwtDecoder,
    private val kakaoOauthProperties: KakaoOauthProperties,
) : SocialTokenVerifier {
    override val provider: SocialProvider = SocialProvider.KAKAO

    override fun verify(idToken: String): SocialIdentity {
        if (kakaoOauthProperties.clientId.isBlank()) {
            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
        val jwt =
            try {
                kakaoJwtDecoder.decode(idToken)
            } catch (exception: JwtException) {
                throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
            }
        val socialId =
            jwt.subject?.takeIf(String::isNotBlank)
                ?: throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)

        return SocialIdentity(provider = SocialProvider.KAKAO, socialId = socialId)
    }
}
