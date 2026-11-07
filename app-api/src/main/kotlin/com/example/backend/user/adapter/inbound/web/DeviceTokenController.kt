package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.DeviceTokenRequest
import com.example.backend.user.application.port.inbound.DeviceTokenUseCase
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.web.AccessTokenRequired
import com.example.backend.web.CurrentDeviceId
import com.example.backend.web.CurrentUserId
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/my/devices")
class DeviceTokenController(
    private val deviceTokenUseCase: DeviceTokenUseCase,
) {
    @PutMapping
    @AccessTokenRequired
    fun register(
        @CurrentUserId userId: Long,
        @CurrentDeviceId deviceId: String,
        @Valid @RequestBody request: DeviceTokenRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        val command =
            RegisterDeviceTokenCommand(
                deviceId,
                request.fcmToken,
            )
        if (!mock) deviceTokenUseCase.register(userId, command)
        return ApiResponse.ok()
    }

    @DeleteMapping
    @AccessTokenRequired
    fun unregister(
        @CurrentUserId userId: Long,
        @CurrentDeviceId deviceId: String,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (!mock) deviceTokenUseCase.unregister(userId, deviceId)
        return ApiResponse.ok()
    }
}
