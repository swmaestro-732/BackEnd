package com.example.backend.bootstrap.device

import com.example.backend.bootstrap.appversion.AppPlatform
import com.example.backend.bootstrap.appversion.AppVersionInterceptor.Companion.BUILD_HEADER
import com.example.backend.bootstrap.appversion.AppVersionInterceptor.Companion.PLATFORM_HEADER
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentAppClientArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentAppClient::class.java) &&
            parameter.parameterType == AppClient::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AppClient =
        AppClient(
            platform =
                webRequest.getHeader(PLATFORM_HEADER)?.let {
                    AppPlatform.fromHeader(it)?.name ?: throw BusinessException(CommonErrorCode.INVALID_APP_HEADER)
                },
            build =
                webRequest.getHeader(BUILD_HEADER)?.let {
                    it.toIntOrNull() ?: throw BusinessException(CommonErrorCode.INVALID_APP_HEADER)
                },
        )
}
