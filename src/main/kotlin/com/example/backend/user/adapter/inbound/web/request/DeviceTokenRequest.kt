package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class DeviceTokenRequest(
    @field:NotBlank
    @field:Size(max = 1024)
    val token: String,
) {
    override fun toString(): String = "DeviceTokenRequest(token=[REDACTED])"
}
