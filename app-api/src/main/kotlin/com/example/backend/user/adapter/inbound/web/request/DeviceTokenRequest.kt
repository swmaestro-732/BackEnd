package com.example.backend.user.adapter.inbound.web.request

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** FCM 토큰은 `fcmToken` 으로 받는다. 이전 앱 빌드가 보내는 `token` 도 전환 기간 동안 함께 받는다. */
data class DeviceTokenRequest(
    @field:NotBlank
    @field:Size(max = 1024)
    // FCM 토큰은 ASCII 라 공백 없는 출력 가능 ASCII 만 받는다. 멀티바이트 문자로 유니크 인덱스 항목 크기를 넘겨 500 이 나지 않게 한다.
    @field:Pattern(regexp = "^[\\x21-\\x7E]+$")
    @param:JsonAlias("token")
    val fcmToken: String,
) {
    override fun toString(): String = "DeviceTokenRequest(fcmToken=[REDACTED])"
}
