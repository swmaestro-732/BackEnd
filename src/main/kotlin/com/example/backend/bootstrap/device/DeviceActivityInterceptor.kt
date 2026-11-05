package com.example.backend.bootstrap.device

import com.example.backend.bootstrap.device.CurrentDeviceIdArgumentResolver.Companion.DEVICE_ID_HEADER
import com.example.backend.bootstrap.device.CurrentDeviceIdArgumentResolver.Companion.MAX_DEVICE_ID_LENGTH
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/**
 * `X-Device-Id` 를 보낸 모든 API 요청을 리텐션 측정용 일별 활동으로 남긴다(SCRUM-783).
 * 헤더가 없거나 형식이 틀리면 건너뛴다. 쓰기는 비동기라 기록이 늦거나 실패해도 요청에 영향이 없다.
 */
@Component
class DeviceActivityInterceptor(
    private val deviceActivityRecorder: DeviceActivityRecorder,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        val deviceId = request.getHeader(DEVICE_ID_HEADER)
        if (deviceId.isNullOrBlank() || deviceId.length > MAX_DEVICE_ID_LENGTH) return true

        deviceActivityRecorder.record(deviceId, currentUserId())
        return true
    }

    private fun currentUserId(): Long? =
        (SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken)
            ?.let { it.principal as? Jwt }
            ?.subject
            ?.toLongOrNull()
}
