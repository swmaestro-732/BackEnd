package com.example.backend.bootstrap.mock

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * 운영(prod 프로파일)에서 모든 웹 컨트롤러의 `mock` 인자를 false 로 바꿔 호출한다 — `?mock=true` 가 와도 실제 로직을 탄다.
 *
 * 목 응답은 엔드포인트마다 타입이 달라 컨트롤러의 `if (mock)` 분기에 그대로 두고, 운영 차단만 이 아스펙트로 모았다.
 * 컨트롤러마다 가드를 호출하던 방식은 새 분기에서 가드를 빠뜨리기 쉬웠다(auth 목이 운영에서 개발 토큰을 발급하던 문제).
 * **운영 배포는 `SPRING_PROFILES_ACTIVE=prod` 로 실행해야 이 차단이 유효**하다(그 외 환경은 개발로 보고 mock 허용).
 */
@Aspect
@Component
class MockGuardAspect(
    private val environment: Environment,
) {
    @Around(
        "@within(org.springframework.web.bind.annotation.RestController) && " +
            "execution(* com.example.backend..adapter.inbound.web..*(..))",
    )
    fun blockMockInProd(joinPoint: ProceedingJoinPoint): Any? {
        if (!isProd(environment)) return joinPoint.proceed()
        val names = (joinPoint.signature as MethodSignature).parameterNames
        val args = joinPoint.args.mapIndexed { i, arg -> if (names[i] == MOCK_PARAM && arg == true) false else arg }
        return joinPoint.proceed(args.toTypedArray())
    }

    companion object {
        private const val MOCK_PARAM = "mock"
        private const val PROD_PROFILE = "prod"

        fun isProd(environment: Environment): Boolean = PROD_PROFILE in environment.activeProfiles
    }
}
