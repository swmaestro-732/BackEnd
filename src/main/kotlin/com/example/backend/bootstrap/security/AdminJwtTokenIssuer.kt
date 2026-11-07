package com.example.backend.bootstrap.security

import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.admin.application.port.outbound.IssuedAdminToken
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.temporal.ChronoUnit

/** 어드민 토큰 발급. 앱 토큰과 같은 키로 서명하되 purpose=admin 으로 구분해 앱 경로와 섞이지 않게 한다. */
@Component
class AdminJwtTokenIssuer(
    private val jwtEncoder: JwtEncoder,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) : AdminTokenPort {
    override fun issue(
        email: String,
        googleSub: String,
    ): IssuedAdminToken {
        // JWT 의 iat/exp 는 초 단위라, 응답의 expiresAt 이 토큰 exp 와 같도록 초 단위로 맞춘다.
        val now = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        val expiresAt = now.plus(jwtProperties.adminTtl)
        val claims =
            JwtClaimsSet
                .builder()
                .subject(email)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(PURPOSE_CLAIM, ADMIN_PURPOSE)
                .claim(GOOGLE_SUB_CLAIM, googleSub)
                .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return IssuedAdminToken(token = token, expiresAt = expiresAt)
    }

    companion object {
        private const val PURPOSE_CLAIM = "purpose"
        private const val ADMIN_PURPOSE = "admin"

        /** 어드민 토큰을 받은 구글 계정 id. 요청별 인가가 지금 묶인 계정과 비교한다. */
        const val GOOGLE_SUB_CLAIM = "google_sub"
    }
}
