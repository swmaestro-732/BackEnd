package com.example.backend.bootstrap.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AdminJwtTokenIssuerTest {
    private val secretKey = SecretKeySpec(SECRET.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
    private val decoder =
        NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()

    // 만료 검증에 걸리지 않게 현재 시각 기준으로 고정한다. 밀리초는 잘려 나가는지 보려고 남긴다.
    private val now = Instant.now().plusMillis(500)
    private val issuer =
        AdminJwtTokenIssuer(
            jwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey)),
            jwtProperties =
                JwtProperties(
                    secret = SECRET,
                    accessTtl = Duration.ofMinutes(30),
                    refreshTtl = Duration.ofDays(14),
                    registrationTtl = Duration.ofMinutes(10),
                    adminTtl = ADMIN_TTL,
                ),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    @Test
    fun `purpose=admin, subject=이메일, exp=iat+adminTtl 로 발급한다`() {
        val issued = issuer.issue(EMAIL)

        val decoded = decoder.decode(issued.token)

        assertEquals("admin", decoded.getClaimAsString("purpose"))
        assertEquals(EMAIL, decoded.subject)
        assertEquals(decoded.issuedAt!!.plus(ADMIN_TTL), decoded.expiresAt)
        assertEquals(decoded.expiresAt, issued.expiresAt)
        assertFalse(issued.toString().contains(issued.token))
    }

    @Test
    fun `adminTtl 기본값은 1시간이다`() {
        val properties =
            JwtProperties(
                secret = SECRET,
                accessTtl = Duration.ofMinutes(30),
                refreshTtl = Duration.ofDays(14),
                registrationTtl = Duration.ofMinutes(10),
            )

        assertEquals(Duration.ofHours(1), properties.adminTtl)
    }

    private companion object {
        const val EMAIL = "admin@example.com"
        const val SECRET = "admin-test-secret-key-with-at-least-thirty-two-bytes"
        val ADMIN_TTL: Duration = Duration.ofMinutes(45)
    }
}
