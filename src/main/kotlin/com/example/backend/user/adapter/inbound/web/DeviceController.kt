package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.device.CurrentDeviceId
import com.example.backend.bootstrap.device.NotUserActivity
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.DeviceRequest
import com.example.backend.user.adapter.inbound.web.response.DeviceConsentResponse
import com.example.backend.user.application.port.inbound.DeviceUseCase
import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.UpdateDeviceCommand
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 로그인과 무관한 기기 단위 푸시 등록과 광고성 수신 동의. 로그인 계정 연결은 [DeviceTokenController] 가 한다.
 * 토큰 갱신은 앱이 백그라운드에서도 부르므로 리텐션 활동으로 세지 않는다.
 */
@NotUserActivity
@RestController
@RequestMapping("/api/v1/devices")
class DeviceController(
    private val deviceUseCase: DeviceUseCase,
) {
    @PutMapping
    fun update(
        @CurrentDeviceId deviceId: String,
        @Valid @RequestBody request: DeviceRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<DeviceConsentResponse> {
        val command =
            UpdateDeviceCommand(
                deviceId,
                request.fcmToken,
                request.marketing,
                request.night,
            )
        val consent = if (mock) DeviceConsent.NONE else deviceUseCase.update(command)
        return ApiResponse.success(DeviceConsentResponse.from(consent))
    }

    @GetMapping
    fun consent(
        @CurrentDeviceId deviceId: String,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<DeviceConsentResponse> {
        val consent = if (mock) DeviceConsent.NONE else deviceUseCase.getConsent(deviceId)
        return ApiResponse.success(DeviceConsentResponse.from(consent))
    }
}
