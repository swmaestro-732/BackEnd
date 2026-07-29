package com.example.backend.bootstrap.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTtl: Duration,
    val refreshTtl: Duration,
) {
    init {
        // 시크릿 미설정/취약 시 기동 실패(fail-fast) — 코드에 기본값을 두지 않고 반드시 env로 주입.
        require(secret.toByteArray().size >= MIN_SECRET_BYTES) {
            "JWT 서명 시크릿이 없거나 너무 짧습니다. JWT_SECRET 환경변수를 $MIN_SECRET_BYTES 바이트 이상으로 설정하세요."
        }
    }

    private companion object {
        const val MIN_SECRET_BYTES = 32
    }
}
