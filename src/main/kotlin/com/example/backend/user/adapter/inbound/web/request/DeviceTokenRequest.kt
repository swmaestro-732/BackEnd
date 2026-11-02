package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** JSON 필드 이름 `token` 은 Android 와 맞춘 API 계약이다. 서버 안에서는 FCM 토큰(fcmToken)으로 다룬다. */
data class DeviceTokenRequest(
    @field:NotBlank
    @field:Size(max = 1024)
    // FCM 토큰은 ASCII 라 공백 없는 출력 가능 ASCII 만 받는다. 멀티바이트 문자로 유니크 인덱스 항목 크기를 넘겨 500 이 나지 않게 한다.
    @field:Pattern(regexp = "^[\\x21-\\x7E]+$")
    val token: String,
) {
    override fun toString(): String = "DeviceTokenRequest(token=[REDACTED])"
}
