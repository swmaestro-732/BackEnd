package com.example.backend.bootstrap.mock

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * mock 응답 허용 여부 가드.
 *
 * 운영(prod 프로파일)에서는 `?mock=true` 를 무시하고 실제 로직을 태운다 — mock 분기가 운영에서
 * 가짜 데이터를 반환하지 못하게 한다. **운영 배포는 `SPRING_PROFILES_ACTIVE=prod` 로 실행해야
 * 이 가드가 유효**하다(그 외 환경은 개발로 보고 mock 허용).
 */
@Component
class MockGuard(
    private val environment: Environment,
) {
    fun isMockAllowed(): Boolean = PROD_PROFILE !in environment.activeProfiles

    private companion object {
        const val PROD_PROFILE = "prod"
    }
}
