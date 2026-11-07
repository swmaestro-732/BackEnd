package com.example.backend.web.device

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.web.CurrentDeviceId
import com.example.backend.web.DEVICE_ID_HEADER
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentDeviceIdArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentDeviceId::class.java) && parameter.parameterType == String::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): String {
        val value = webRequest.getHeader(DEVICE_ID_HEADER)
        if (value.isNullOrBlank() || value.length > MAX_DEVICE_ID_LENGTH) {
            throw BusinessException(CommonErrorCode.INVALID_INPUT, "$DEVICE_ID_HEADER 헤더가 비었거나 너무 깁니다.")
        }
        return value
    }

    private companion object {
        const val MAX_DEVICE_ID_LENGTH = 128
    }
}
