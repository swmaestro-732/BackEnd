package com.example.backend.user.adapter.outbound.social

import com.example.backend.bootstrap.security.AppleOauthProperties
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

class AppleSocialVerifierTest {
    @Test
    fun `유효한 ID 토큰의 subject를 socialId로 복원한다`() {
        val verifier = verifier(decoder = JwtDecoder { jwtWithSubject("apple-sub-123") })

        val identity = verifier.verify("valid-token")

        assertEquals(SocialProvider.APPLE, identity.provider)
        assertEquals("apple-sub-123", identity.socialId)
    }

    @Test
    fun `Bundle ID·Services ID가 모두 비어있으면 검증하지 않고 거부한다`() {
        val verifier =
            verifier(
                properties = properties(clientId = ""),
                decoder = JwtDecoder { throw AssertionError("decoder must not be called") },
            )

        val exception = assertFailsWith<BusinessException> { verifier.verify("any-token") }

        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
    }

    @Test
    fun `Bundle ID가 없어도 Services ID만 있으면 검증을 진행한다(웹·안드로이드 전용)`() {
        val verifier =
            verifier(
                properties = properties(clientId = "", serviceId = "service-id"),
                decoder = JwtDecoder { jwtWithSubject("apple-web-sub") },
            )

        val identity = verifier.verify("web-token")

        assertEquals(SocialProvider.APPLE, identity.provider)
        assertEquals("apple-web-sub", identity.socialId)
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
        properties: AppleOauthProperties = properties(),
        decoder: JwtDecoder,
    ): AppleSocialVerifier = AppleSocialVerifier(appleJwtDecoder = decoder, appleOauthProperties = properties)

    private fun properties(
        clientId: String = "com.example.app",
        serviceId: String = "",
    ): AppleOauthProperties =
        AppleOauthProperties(
            clientId = clientId,
            serviceId = serviceId,
            jwksUri = "https://appleid.apple.com/auth/keys",
            issuer = "https://appleid.apple.com",
        )

    private fun jwtWithSubject(subject: String?): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "RS256")
            .apply { if (subject != null) subject(subject) else claim("sub", "") }
            .claim("iss", "https://appleid.apple.com")
            .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
            .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
            .build()
}
