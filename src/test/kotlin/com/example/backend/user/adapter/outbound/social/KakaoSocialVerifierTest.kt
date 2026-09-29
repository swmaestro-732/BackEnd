package com.example.backend.user.adapter.outbound.social

import com.example.backend.bootstrap.security.KakaoOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.domain.model.SocialProvider
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KakaoSocialVerifierTest {
    @Test
    fun `유효한 ID 토큰의 subject를 socialId로 복원한다`() {
        val verifier = verifier(decoder = JwtDecoder { jwtWithSubject("kakao-sub-123") })

        val identity = verifier.verify("valid-token")

        assertEquals(SocialProvider.KAKAO, identity.provider)
        assertEquals("kakao-sub-123", identity.socialId)
    }

    @Test
    fun `client-id가 비어있으면 검증하지 않고 거부한다`() {
        val verifier =
            verifier(
                properties = properties(clientId = ""),
                decoder = JwtDecoder { throw AssertionError("decoder must not be called") },
            )

        val exception = assertFailsWith<BusinessException> { verifier.verify("any-token") }

        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
    }

    @Test
    fun `decoder가 JwtException을 던지면 인증 실패로 변환한다`() {
        val verifier = verifier(decoder = JwtDecoder { throw JwtException("invalid signature") })

        val exception = assertFailsWith<BusinessException> { verifier.verify("bad-token") }

        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
    }

    @Test
    fun `subject가 없으면 인증 실패로 처리한다`() {
        val verifier = verifier(decoder = JwtDecoder { jwtWithSubject(null) })

        val exception = assertFailsWith<BusinessException> { verifier.verify("no-sub-token") }

        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
    }

    private fun verifier(
        properties: KakaoOauthProperties = properties(),
        decoder: JwtDecoder,
    ): KakaoSocialVerifier = KakaoSocialVerifier(kakaoJwtDecoder = decoder, kakaoOauthProperties = properties)

    private fun properties(clientId: String = "kakao-rest-key"): KakaoOauthProperties =
        KakaoOauthProperties(
            clientId = clientId,
            jwksUri = "https://kauth.kakao.com/.well-known/jwks.json",
            issuer = "https://kauth.kakao.com",
        )

    private fun jwtWithSubject(subject: String?): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "RS256")
            .apply { if (subject != null) subject(subject) else claim("sub", "") }
            .claim("iss", "https://kauth.kakao.com")
            .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
            .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
            .build()
}
