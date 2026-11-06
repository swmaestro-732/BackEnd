package com.example.backend.user.application.port.inbound.dto

data class RegisterDeviceTokenCommand(
    val deviceId: String,
    val fcmToken: String,
) {
    override fun toString(): String = "RegisterDeviceTokenCommand(deviceId=$deviceId, fcmToken=[REDACTED])"
}
