package com.example.backend.bootstrap.security

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.outbound.AuthTokenPort
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.domain.model.SocialProvider
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class JwtTokenProvider(
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: JwtDecoder,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) : AuthTokenPort {
    override fun issueAccessToken(userId: Long): String {
        val now = clock.instant()
        val claims =
            JwtClaimsSet
                .builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTtl))
                .claim(PURPOSE_CLAIM, ACCESS_PURPOSE)
                .build()

        return encode(claims)
    }

    override fun issueRegistrationToken(
        provider: SocialProvider,
        socialId: String,
    ): String {
        val now = clock.instant()
        val claims =
            JwtClaimsSet
                .builder()
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.registrationTtl))
                .claim(PURPOSE_CLAIM, REGISTRATION_PURPOSE)
                .claim(PROVIDER_CLAIM, provider.name)
                .claim(SOCIAL_ID_CLAIM, socialId)
                .build()

        return encode(claims)
    }

    override fun parseRegistrationToken(token: String): SocialIdentity {
        val jwt =
            try {
                jwtDecoder.decode(token)
            } catch (exception: JwtException) {
                throw BusinessException(ErrorCode.INVALID_REGISTRATION_TOKEN)
            }

        if (jwt.getClaimAsString(PURPOSE_CLAIM) != REGISTRATION_PURPOSE) {
            throw BusinessException(ErrorCode.INVALID_REGISTRATION_TOKEN)
        }
        val providerClaim =
            jwt.getClaimAsString(PROVIDER_CLAIM)?.takeIf(String::isNotBlank)
                ?: throw BusinessException(ErrorCode.INVALID_REGISTRATION_TOKEN)
        val provider =
            runCatching { SocialProvider.valueOf(providerClaim) }
                .getOrElse { throw BusinessException(ErrorCode.INVALID_REGISTRATION_TOKEN) }
        val socialId =
            jwt.getClaimAsString(SOCIAL_ID_CLAIM)?.takeIf(String::isNotBlank)
                ?: throw BusinessException(ErrorCode.INVALID_REGISTRATION_TOKEN)

        return SocialIdentity(provider = provider, socialId = socialId)
    }

    private fun encode(claims: JwtClaimsSet): String {
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    private companion object {
        const val PURPOSE_CLAIM = "purpose"
        const val PROVIDER_CLAIM = "provider"
        const val SOCIAL_ID_CLAIM = "socialId"
        const val ACCESS_PURPOSE = "access"
        const val REGISTRATION_PURPOSE = "registration"
    }
}
