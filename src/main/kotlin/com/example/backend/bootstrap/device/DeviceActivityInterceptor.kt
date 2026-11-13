package com.example.backend.bootstrap.device

import com.example.backend.bootstrap.appversion.AppPlatform
import com.example.backend.bootstrap.appversion.AppVersionInterceptor.Companion.PLATFORM_HEADER
import com.example.backend.bootstrap.device.CurrentDeviceIdArgumentResolver.Companion.DEVICE_ID_HEADER
import com.example.backend.bootstrap.device.CurrentDeviceIdArgumentResolver.Companion.MAX_DEVICE_ID_LENGTH
import com.example.backend.user.application.port.inbound.DeviceActivityUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/**
 * `X-Device-Id` 를 보낸 모든 API 요청을 리텐션 측정용 일별 활동으로 남긴다(SCRUM-783). `X-App-Platform` 으로 OS 도 남긴다.
 * 헤더가 없거나 형식이 틀리거나 [NotUserActivity] 가 붙은 API 면 건너뛴다. 쓰기는 비동기라 기록이 늦거나 실패해도 요청에 영향이 없다.
 */
@Component
class DeviceActivityInterceptor(
    private val deviceActivityUseCase: DeviceActivityUseCase,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod || isNotUserActivity(handler)) return true
        val deviceId = request.getHeader(DEVICE_ID_HEADER)
        if (deviceId.isNullOrBlank() || deviceId.length > MAX_DEVICE_ID_LENGTH) return true

        val platform =
            request
                .getHeader(PLATFORM_HEADER)
                ?.let(AppPlatform::fromHeader)
                ?.name
                ?.lowercase()
        deviceActivityUseCase.record(deviceId, currentUserId(), platform)
        return true
    }

    private fun isNotUserActivity(handler: HandlerMethod): Boolean =
        handler.hasMethodAnnotation(NotUserActivity::class.java) ||
            handler.beanType.isAnnotationPresent(NotUserActivity::class.java)

    private fun currentUserId(): Long? =
        (SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken)
            ?.let { it.principal as? Jwt }
            ?.subject
            ?.toLongOrNull()
}
