package com.example.backend.bootstrap.mock.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockAspect
import com.example.backend.common.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * 목 아스펙트의 운영 차단 — 포인트컷(`..adapter.inbound.web..` 의 `@RestController`)에 걸리도록
 * 테스트 컨트롤러를 이 패키지에 두고 프록시로 호출한다.
 */
class MockAspectTest {
    @RestController
    open class SampleController {
        open fun get(
            id: Long,
            mock: Boolean,
        ): String = if (mock) "MOCK" else "REAL:$id"
    }

    @AfterEach
    fun tearDown() = RequestContextHolder.resetRequestAttributes()

    private fun proxy(vararg profiles: String): SampleController {
        val environment = MockEnvironment().apply { setActiveProfiles(*profiles) }
        return AspectJProxyFactory(SampleController())
            .apply {
                isProxyTargetClass = true
                addAspect(MockAspect(environment))
            }.getProxy()
    }

    private fun requestWith(vararg params: Pair<String, String>) {
        val request = MockHttpServletRequest().apply { params.forEach { (k, v) -> addParameter(k, v) } }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @Test
    fun `prod 에서는 mock=true 가 와도 실제 로직을 탄다`() {
        assertThat(proxy("prod").get(7, mock = true)).isEqualTo("REAL:7")
    }

    @Test
    fun `prod 가 아니면 mock=true 로 목 분기를 탄다`() {
        assertThat(proxy("dev").get(7, mock = true)).isEqualTo("MOCK")
    }

    @Test
    fun `prod 에서는 mockError 를 주입하지 않는다`() {
        requestWith("mockError" to "4001")
        assertThat(proxy("prod").get(7, mock = false)).isEqualTo("REAL:7")
    }

    @Test
    fun `prod 가 아니면 mockError 로 에러를 주입한다`() {
        requestWith("mockError" to "4001")
        assertThatThrownBy { proxy().get(7, mock = false) }.isInstanceOf(BusinessException::class.java)
    }
}
