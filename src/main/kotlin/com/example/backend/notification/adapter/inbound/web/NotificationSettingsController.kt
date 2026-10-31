package com.example.backend.notification.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.notification.adapter.inbound.web.request.UpdateNotificationSettingsRequest
import com.example.backend.notification.adapter.inbound.web.response.NotificationSettingsResponse
import com.example.backend.notification.application.port.inbound.NotificationSettingsUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/my/notification-settings")
@AccessTokenRequired
@RequiresAppFeature(AppFeature.NOTIFICATION)
class NotificationSettingsController(
    private val useCase: NotificationSettingsUseCase,
) {
    @GetMapping
    fun get(
        @CurrentUserId userId: Long,
    ): ApiResponse<NotificationSettingsResponse> =
        ApiResponse.success(NotificationSettingsResponse.from(useCase.get(userId)))

    @PatchMapping
    fun update(
        @CurrentUserId userId: Long,
        @RequestBody request: UpdateNotificationSettingsRequest,
    ): ApiResponse<NotificationSettingsResponse> =
        ApiResponse.success(NotificationSettingsResponse.from(useCase.update(userId, request.toCommand())))
}
