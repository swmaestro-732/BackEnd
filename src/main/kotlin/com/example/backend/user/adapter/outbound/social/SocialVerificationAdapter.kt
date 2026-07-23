package com.example.backend.user.adapter.outbound.social

import com.example.backend.bootstrap.security.KakaoOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.domain.model.SocialProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

/** Kakao ID 토큰을 Stage 1의 JWK 기반 decoder 로 검증한다. */
@Component
class SocialVerificationAdapter(
    @param:Qualifier("kakaoJwtDecoder")
    private val kakaoJwtDecoder: JwtDecoder,
    private val kakaoOauthProperties: KakaoOauthProperties,
) : SocialVerificationPort {
    override fun verify(
        provider: SocialProvider,
        idToken: String,
    ): SocialIdentity {
        if (provider != SocialProvider.KAKAO || kakaoOauthProperties.clientId.isBlank()) {
            throw BusinessException(ErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
        val jwt =
            try {
                kakaoJwtDecoder.decode(idToken)
            } catch (exception: JwtException) {
                throw BusinessException(ErrorCode.SOCIAL_AUTHENTICATION_FAILED)
            }
        val socialId =
            jwt.subject?.takeIf(String::isNotBlank)
                ?: throw BusinessException(ErrorCode.SOCIAL_AUTHENTICATION_FAILED)

        return SocialIdentity(provider = SocialProvider.KAKAO, socialId = socialId)
    }
}
