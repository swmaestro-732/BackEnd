package com.example.backend.bootstrap.mock

import com.example.backend.common.mock.MockErrors
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * 모든 웹 컨트롤러의 모킹 동작을 한곳에서 다룬다.
 *
 * - 개발 환경: `?mockError=<code>` 쿼리를 읽어 해당 에러를 던진다(컨트롤러마다 반복하던 [MockErrors.throwIfRequested] 를 추출).
 * - 운영(prod 프로파일): `mockError` 를 무시하고, 컨트롤러의 `mock` 인자를 false 로 바꿔 호출한다 — `?mock=true` 가 와도 실제 로직을 탄다.
 *
 * 목 응답은 엔드포인트마다 타입이 달라 컨트롤러의 `if (mock)` 분기에 그대로 두고, 운영 차단만 여기로 모았다.
 * 컨트롤러마다 가드를 호출하던 방식은 새 분기에서 가드를 빠뜨리기 쉬웠다(auth 목이 운영에서 개발 토큰을 발급하던 문제).
 * **운영 배포는 `SPRING_PROFILES_ACTIVE=prod` 로 실행해야 이 차단이 유효**하다(그 외 환경은 개발로 보고 mock 허용).
 * 모킹을 전부 걷어낼 때 이 아스펙트도 함께 제거한다.
 */
@Aspect
@Component
class MockAspect(
    private val environment: Environment,
) {
    @Around(
        "@within(org.springframework.web.bind.annotation.RestController) && " +
            "execution(* com.example.backend..adapter.inbound.web..*(..))",
    )
    fun handleMock(joinPoint: ProceedingJoinPoint): Any? {
        if (PROD_PROFILE !in environment.activeProfiles) {
            injectMockError()
            return joinPoint.proceed()
        }
        val names = (joinPoint.signature as MethodSignature).parameterNames
        val args = joinPoint.args.mapIndexed { i, arg -> if (names[i] == MOCK_PARAM && arg == true) false else arg }
        return joinPoint.proceed(args.toTypedArray())
    }

    private fun injectMockError() {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return
        val mockError = attributes.request.getParameter("mockError")?.toIntOrNull() ?: return
        MockErrors.throwIfRequested(mockError)
    }

    private companion object {
        const val MOCK_PARAM = "mock"
        const val PROD_PROFILE = "prod"
    }
}
