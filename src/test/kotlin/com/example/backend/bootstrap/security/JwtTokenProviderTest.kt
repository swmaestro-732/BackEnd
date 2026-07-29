package com.example.backend.bootstrap.security

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.domain.model.SocialProvider
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
import java.time.Instant
import java.time.ZoneOffset
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtTokenProviderTest {
    private val decoder = createDecoder(PRIMARY_SECRET)
    private val provider = createProvider(PRIMARY_SECRET, Clock.systemUTC())

    @Test
    fun `access token 발급 후 subject로 사용자 ID를 복원한다`() {
        val token = provider.issueAccessToken(USER_ID)

        val decoded = decoder.decode(token)

        assertEquals(USER_ID.toString(), decoded.subject)
    }

    @Test
    fun `registration token에서 provider와 socialId를 복원한다`() {
        val token = provider.issueRegistrationToken(SocialProvider.KAKAO, SOCIAL_ID)

        val identity = provider.parseRegistrationToken(token)

        assertEquals(SocialProvider.KAKAO, identity.provider)
        assertEquals(SOCIAL_ID, identity.socialId)
    }

    @Test
    fun `만료된 registration token을 거부한다`() {
        val expiredProvider =
            createProvider(
                PRIMARY_SECRET,
                Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            )
        val token = expiredProvider.issueRegistrationToken(SocialProvider.KAKAO, SOCIAL_ID)

        val exception = assertFailsWith<BusinessException> { provider.parseRegistrationToken(token) }

        assertEquals(ErrorCode.INVALID_REGISTRATION_TOKEN, exception.errorCode)
    }

    @Test
    fun `다른 키로 서명한 위조 registration token을 거부한다`() {
        val forgedProvider = createProvider(FORGED_SECRET, Clock.systemUTC())
        val token = forgedProvider.issueRegistrationToken(SocialProvider.KAKAO, SOCIAL_ID)

        val exception = assertFailsWith<BusinessException> { provider.parseRegistrationToken(token) }

        assertEquals(ErrorCode.INVALID_REGISTRATION_TOKEN, exception.errorCode)
    }

    private fun createProvider(
        secret: String,
        clock: Clock,
    ): JwtTokenProvider =
        JwtTokenProvider(
            jwtEncoder = createEncoder(secret),
            jwtDecoder = createDecoder(secret),
            jwtProperties =
                JwtProperties(
                    secret = secret,
                    accessTtl = Duration.ofMinutes(30),
                    refreshTtl = Duration.ofDays(14),
                    registrationTtl = Duration.ofMinutes(10),
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
        const val SOCIAL_ID = "kakao-social-id"
        const val PRIMARY_SECRET = "primary-test-secret-key-with-at-least-thirty-two-bytes"
        const val FORGED_SECRET = "forged-test-secret-key-with-at-least-thirty-two-bytes-0"
    }
}
