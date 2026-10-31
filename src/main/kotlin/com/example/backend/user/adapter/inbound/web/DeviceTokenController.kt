package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppPlatform
import com.example.backend.bootstrap.appversion.AppVersionInterceptor.Companion.BUILD_HEADER
import com.example.backend.bootstrap.appversion.AppVersionInterceptor.Companion.PLATFORM_HEADER
import com.example.backend.bootstrap.appversion.CurrentDeviceId
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.adapter.inbound.web.request.DeviceTokenRequest
import com.example.backend.user.application.port.inbound.DeviceTokenUseCase
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
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
        @RequestHeader(PLATFORM_HEADER, required = false) platform: String?,
        @RequestHeader(BUILD_HEADER, required = false) appBuild: String?,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        val command =
            RegisterDeviceTokenCommand(deviceId, request.fcmToken, parsePlatform(platform), parseBuild(appBuild))
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

    private fun parsePlatform(value: String?): String? =
        value?.let {
            AppPlatform.fromHeader(it)?.name ?: throw BusinessException(CommonErrorCode.INVALID_APP_HEADER)
        }

    private fun parseBuild(value: String?): Int? =
        value?.let {
            it.toIntOrNull() ?: throw BusinessException(CommonErrorCode.INVALID_APP_HEADER)
        }
}
