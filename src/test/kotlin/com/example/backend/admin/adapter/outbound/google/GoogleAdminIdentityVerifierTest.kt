package com.example.backend.admin.adapter.outbound.google

import com.example.backend.admin.application.port.outbound.VerifiedGoogleAccount
import com.example.backend.bootstrap.security.GoogleOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GoogleAdminIdentityVerifierTest {
    @Test
    fun `web client-id aud 이고 인증된 이메일이면 소문자 이메일과 구글 sub 를 돌려준다`() {
        val verifier = verifier(decoder = JwtDecoder { jwt(email = "Admin@Example.com") })

        assertEquals(VerifiedGoogleAccount("admin@example.com", "google-sub"), verifier.verifyGoogle("valid-token"))
    }

    @Test
    fun `email_verified 가 문자열 true 여도 통과한다`() {
        val verifier = verifier(decoder = JwtDecoder { jwt(emailVerified = "true") })

        assertEquals("admin@example.com", verifier.verifyGoogle("valid-token").email)
    }

    @Test
    fun `android client-id aud 만 있는 토큰은 거부한다`() {
        assertRejected(verifier(decoder = JwtDecoder { jwt(audience = listOf(ANDROID_CLIENT_ID)) }))
    }

    @ParameterizedTest
    @ValueSource(strings = ["false", "boolean-false", "missing"])
    fun `email_verified 가 false 이거나 없으면 거부한다`(case: String) {
        val emailVerified: Any? =
            when (case) {
                "false" -> "false"
                "boolean-false" -> false
                else -> null
            }

        assertRejected(verifier(decoder = JwtDecoder { jwt(emailVerified = emailVerified) }))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `email 이 비어 있으면 거부한다`(email: String) {
        assertRejected(verifier(decoder = JwtDecoder { jwt(email = email) }))
    }

    @Test
    fun `email 클레임이 없으면 거부한다`() {
        assertRejected(verifier(decoder = JwtDecoder { jwt(email = null) }))
    }

    @Test
    fun `sub 가 비어 있으면 거부한다`() {
        assertRejected(verifier(decoder = JwtDecoder { jwt(subject = " ") }))
    }

    @Test
    fun `decoder 가 JwtException 을 던지면 인증 실패로 변환한다`() {
        assertRejected(verifier(decoder = JwtDecoder { throw JwtException("invalid signature") }))
    }

    @Test
    fun `web client-id 가 비어 있으면 decoder 를 호출하지 않고 거부한다`() {
        assertRejected(
            verifier(
                clientId = "",
                decoder = JwtDecoder { throw AssertionError("decoder must not be called") },
            ),
        )
    }

    private fun assertRejected(verifier: GoogleAdminIdentityVerifier) {
        val exception = assertFailsWith<BusinessException> { verifier.verifyGoogle("token") }
        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
    }

    private fun verifier(
        clientId: String = WEB_CLIENT_ID,
        decoder: JwtDecoder,
    ): GoogleAdminIdentityVerifier =
        GoogleAdminIdentityVerifier(
            googleJwtDecoder = decoder,
            googleOauthProperties =
                GoogleOauthProperties(
                    clientId = clientId,
                    androidClientId = ANDROID_CLIENT_ID,
                    jwksUri = "https://www.googleapis.com/oauth2/v3/certs",
                    issuer = "https://accounts.google.com",
                ),
        )

    private fun jwt(
        audience: List<String> = listOf(WEB_CLIENT_ID),
        email: String? = "admin@example.com",
        emailVerified: Any? = true,
        subject: String = "google-sub",
    ): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "RS256")
            .subject(subject)
            .audience(audience)
            .claim("iss", "https://accounts.google.com")
            .apply { if (email != null) claim("email", email) }
            .apply { if (emailVerified != null) claim("email_verified", emailVerified) }
            .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
            .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
            .build()

    private companion object {
        const val WEB_CLIENT_ID = "web-client-id"
        const val ANDROID_CLIENT_ID = "android-client-id"
    }
}
