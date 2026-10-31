package com.example.backend.notification.application.port.inbound.dto

data class RegisterDeviceTokenCommand(
    val token: String,
    val platform: String?,
    val appBuild: Int?,
) {
    override fun toString(): String =
        "RegisterDeviceTokenCommand(token=[REDACTED], platform=$platform, appBuild=$appBuild)"
}
