package com.example.backend.bootstrap.device

import com.example.backend.bootstrap.device.CurrentDeviceIdArgumentResolver.Companion.DEVICE_ID_HEADER
import com.example.backend.bootstrap.device.CurrentDeviceIdArgumentResolver.Companion.MAX_DEVICE_ID_LENGTH
import io.github.oshai.kotlinlogging.KotlinLogging
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
 * 헤더가 없거나 형식이 틀리면 건너뛰고, 기록이 실패해도 요청은 그대로 처리한다.
 */
@Component
class DeviceActivityInterceptor(
    private val deviceActivityRecorder: DeviceActivityRecorder,
) : HandlerInterceptor {
    private val log = KotlinLogging.logger {}

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        val deviceId = request.getHeader(DEVICE_ID_HEADER)
        if (deviceId.isNullOrBlank() || deviceId.length > MAX_DEVICE_ID_LENGTH) return true

        runCatching { deviceActivityRecorder.record(deviceId, currentUserId()) }
            .onFailure { log.warn(it) { "기기 활동 기록 실패: deviceId=$deviceId" } }
        return true
    }

    private fun currentUserId(): Long? =
        (SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken)
            ?.let { it.principal as? Jwt }
            ?.subject
            ?.toLongOrNull()
}
