package com.example.backend.user.application.port.inbound.dto

/** 비로그인 기기 갱신. null 필드는 바꾸지 않는다. */
data class UpdateDeviceCommand(
    val deviceId: String,
    val fcmToken: String?,
    val marketing: Boolean?,
    val night: Boolean?,
) {
    override fun toString(): String =
        "UpdateDeviceCommand(deviceId=$deviceId, fcmToken=[REDACTED], marketing=$marketing, night=$night)"
}
