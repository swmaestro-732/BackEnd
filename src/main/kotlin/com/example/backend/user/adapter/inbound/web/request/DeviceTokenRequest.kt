package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** JSON 필드 이름 `token` 은 Android 와 맞춘 API 계약이다. 서버 안에서는 FCM 토큰(fcmToken)으로 다룬다. */
data class DeviceTokenRequest(
    @field:NotBlank
    @field:Size(max = 1024)
    val token: String,
) {
    override fun toString(): String = "DeviceTokenRequest(token=[REDACTED])"
}
