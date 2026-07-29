package com.example.backend.bootstrap.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals

class JwtTokenProviderTest {
    private val decoder = createDecoder(PRIMARY_SECRET)
    private val provider = createProvider(PRIMARY_SECRET, Clock.systemUTC())

    @Test
    fun `access token 발급 후 subject로 사용자 ID를 복원한다`() {
        val token = provider.issueAccessToken(USER_ID)

        val decoded = decoder.decode(token)

        assertEquals(USER_ID.toString(), decoded.subject)
    }

    private fun createProvider(
        secret: String,
        clock: Clock,
    ): JwtTokenProvider =
        JwtTokenProvider(
            jwtEncoder = createEncoder(secret),
            jwtProperties =
                JwtProperties(
                    secret = secret,
                    accessTtl = Duration.ofMinutes(30),
                    refreshTtl = Duration.ofDays(14),
                ),
            clock = clock,
        )

    private fun createEncoder(secret: String): JwtEncoder =
        NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey(secret)))

    private fun createDecoder(secret: String): JwtDecoder =
        NimbusJwtDecoder
            .withSecretKey(secretKey(secret))
            .macAlgorithm(MacAlgorithm.HS256)
            .build()

    private fun secretKey(secret: String) = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")

    private companion object {
        const val USER_ID = 42L
        const val PRIMARY_SECRET = "primary-test-secret-key-with-at-least-thirty-two-bytes"
    }
}
