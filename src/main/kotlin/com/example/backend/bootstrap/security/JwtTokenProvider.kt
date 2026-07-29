package com.example.backend.bootstrap.security

import com.example.backend.user.application.port.outbound.AuthTokenPort
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class JwtTokenProvider(
    private val jwtEncoder: JwtEncoder,
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

    private fun encode(claims: JwtClaimsSet): String {
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    private companion object {
        const val PURPOSE_CLAIM = "purpose"
        const val ACCESS_PURPOSE = "access"
    }
}
