package com.example.backend.user.application.port.outbound

import java.time.Duration

/**
 * refresh token 발급에 필요한 정책 값(만료 기간).
 * 영속성 계층을 Spring Boot 설정 바인딩(@ConfigurationProperties)에서 떼어내기 위한 순수 계약이다.
 * 실제 값은 app-api 의 JwtProperties(@ConfigurationProperties)가 구현해 주입한다.
 */
interface RefreshTokenPolicy {
    val refreshTtl: Duration
}
