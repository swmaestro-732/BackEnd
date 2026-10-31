package com.example.backend.user.application.port.inbound.dto

data class RegisterDeviceTokenCommand(
    val deviceId: String,
    val token: String,
    val platform: String?,
    val appBuild: Int?,
) {
    override fun toString(): String =
        "RegisterDeviceTokenCommand(deviceId=$deviceId, token=[REDACTED], platform=$platform, appBuild=$appBuild)"
}
