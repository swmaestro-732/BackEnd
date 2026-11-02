package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** 비로그인 기기 갱신. 보내지 않은(null) 필드는 바꾸지 않는다. 토큰 규칙은 [DeviceTokenRequest] 와 같다. */
data class DeviceRequest(
    @field:Size(min = 1, max = 1024)
    @field:Pattern(regexp = "^[\\x21-\\x7E]+$")
    val fcmToken: String? = null,
    val marketing: Boolean? = null,
    val night: Boolean? = null,
) {
    override fun toString(): String = "DeviceRequest(fcmToken=[REDACTED], marketing=$marketing, night=$night)"
}
