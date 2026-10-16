package com.example.backend.bootstrap.appversion

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AppVersionInterceptor(
    private val policyProvider: AppVersionPolicyProvider,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true

        val feature = resolveFeature(handler) ?: return true
        val appVersion = parseAppVersion(request) ?: return true
        val minBuild = policyProvider.minBuild(feature, appVersion.platform) ?: return true

        if (appVersion.build < minBuild) {
            throw BusinessException(CommonErrorCode.APP_UPDATE_REQUIRED)
        }
        return true
    }

    private fun parseAppVersion(request: HttpServletRequest): AppVersion? {
        val rawBuild = request.getHeader(BUILD_HEADER)
        val rawPlatform = request.getHeader(PLATFORM_HEADER)
        if (rawBuild == null && rawPlatform == null) return null

        val build = rawBuild?.toIntOrNull()
        val platform = rawPlatform?.let(AppPlatform::fromHeader)
        if (build == null || platform == null) {
            throw BusinessException(CommonErrorCode.INVALID_APP_HEADER)
        }

        return AppVersion(platform, build)
    }

    private data class AppVersion(
        val platform: AppPlatform,
        val build: Int,
    )

    private fun resolveFeature(handler: HandlerMethod): AppFeature? =
        handler.getMethodAnnotation(RequiresAppFeature::class.java)?.value
            ?: handler.beanType.getAnnotation(RequiresAppFeature::class.java)?.value

    private companion object {
        const val BUILD_HEADER = "X-App-Build"
        const val PLATFORM_HEADER = "X-App-Platform"
    }
}
