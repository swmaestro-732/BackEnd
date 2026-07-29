package com.example.backend.bootstrap.mock

import com.example.backend.common.mock.MockErrors
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * 모든 웹 컨트롤러의 `?mockError=<code>` 쿼리를 가로채 모킹 에러를 주입한다.
 * 컨트롤러마다 반복하던 [MockErrors.throwIfRequested] 보일러플레이트를 이 아스펙트로 추출.
 * 실구현 전환·모킹 제거 시 이 아스펙트도 함께 걷어낸다.
 */
@Aspect
@Component
class MockErrorAspect {
    @Before(
        "@within(org.springframework.web.bind.annotation.RestController) && " +
            "execution(* com.example.backend..adapter.inbound.web..*(..))",
    )
    fun injectMockError() {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return
        val mockError = attributes.request.getParameter("mockError")?.toIntOrNull() ?: return
        MockErrors.throwIfRequested(mockError)
    }
}
