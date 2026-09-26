package com.example.backend.user.adapter.outbound.social

import com.example.backend.bootstrap.security.KakaoOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.domain.model.SocialProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

/** 제공자별 토큰을 검증하고 서비스에서 사용하는 소셜 식별자로 변환한다. */
@Component
class SocialVerificationAdapter(
    @param:Qualifier("kakaoJwtDecoder")
    private val kakaoJwtDecoder: JwtDecoder,
    private val kakaoOauthProperties: KakaoOauthProperties,
    private val naverProfileClient: NaverProfileClient,
) : SocialVerificationPort {
    override fun verify(
        provider: SocialProvider,
        token: String,
    ): SocialIdentity {
        if (provider == SocialProvider.NAVER) return naverProfileClient.verify(token)
        if (provider != SocialProvider.KAKAO || kakaoOauthProperties.clientId.isBlank()) {
            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
        val jwt =
            try {
                kakaoJwtDecoder.decode(token)
            } catch (exception: JwtException) {
                throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
            }
        val socialId =
            jwt.subject?.takeIf(String::isNotBlank)
                ?: throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)

        return SocialIdentity(provider = SocialProvider.KAKAO, socialId = socialId)
    }
}
