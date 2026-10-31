package com.example.backend.user

import com.example.backend.user.adapter.inbound.web.request.DeviceTokenRequest
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceTokenRedactionTest {
    @Test
    fun `요청 객체를 문자열로 변환해도 토큰이 노출되지 않는다`() {
        val token = "secret-request-device-token"

        val representation = DeviceTokenRequest(token).toString()

        assertFalse(representation.contains(token))
        assertTrue(representation.contains("[REDACTED]"))
    }

    @Test
    fun `커맨드는 토큰을 숨기고 플랫폼과 빌드는 표시한다`() {
        val token = "secret-command-device-token"

        val representation = RegisterDeviceTokenCommand(token, "ANDROID", 12).toString()

        assertFalse(representation.contains(token))
        assertTrue(representation.contains("[REDACTED]"))
        assertTrue(representation.contains("platform=ANDROID"))
        assertTrue(representation.contains("appBuild=12"))
    }
}
