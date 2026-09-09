package com.example.backend.bootstrap.appversion

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AppVersionInterceptor(
    private val policyProvider: AppVersionPolicyProvider,
    private val meterRegistry: MeterRegistry,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true

        val feature = resolveFeature(handler) ?: return true

        val platform = AppPlatform.fromUserAgent(request.getHeader(HttpHeaders.USER_AGENT))
        val build = request.getHeader(BUILD_HEADER)?.toIntOrNull()
        if (build == null) {
            meterRegistry
                .counter(
                    INVALID_HEADER_METRIC,
                    "feature",
                    feature,
                    "platform",
                    platform.name.lowercase(),
                ).increment()
            throw BusinessException(CommonErrorCode.INVALID_APP_HEADER)
        }

        val minBuild = policyProvider.minBuild(feature, platform) ?: return true
        if (build < minBuild) {
            meterRegistry
                .counter(
                    BLOCKED_METRIC,
                    "feature",
                    feature,
                    "platform",
                    platform.name.lowercase(),
                ).increment()
            throw BusinessException(CommonErrorCode.APP_UPDATE_REQUIRED)
        }
        return true
    }

    private fun resolveFeature(handler: HandlerMethod): String? =
        handler.getMethodAnnotation(RequiresAppFeature::class.java)?.value
            ?: AnnotatedElementUtils.findMergedAnnotation(handler.beanType, RequiresAppFeature::class.java)?.value

    private companion object {
        const val BUILD_HEADER = "X-App-Build"
        const val BLOCKED_METRIC = "app.update.blocked"
        const val INVALID_HEADER_METRIC = "app.header.invalid"
    }
}
