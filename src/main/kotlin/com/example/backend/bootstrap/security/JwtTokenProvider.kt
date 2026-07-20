package com.example.backend.bootstrap.security

import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class JwtTokenProvider(
    private val jwtEncoder: JwtEncoder,
    private val jwtProperties: JwtProperties,
) {
    fun issueAccessToken(userId: Long): String {
        val now = Instant.now()
        val claims =
            JwtClaimsSet
                .builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTtl))
                .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}
