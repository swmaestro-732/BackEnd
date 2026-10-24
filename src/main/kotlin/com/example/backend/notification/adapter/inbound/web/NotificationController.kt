package com.example.backend.notification.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.notification.adapter.inbound.web.response.NotificationCountResponse
import com.example.backend.notification.adapter.inbound.web.response.NotificationListResponse
import com.example.backend.notification.application.port.inbound.NotificationUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/my/notifications")
@AccessTokenRequired
@RequiresAppFeature(AppFeature.NOTIFICATION)
class NotificationController(
    private val useCase: NotificationUseCase,
) {
    @GetMapping
    fun list(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int = 20,
    ): ApiResponse<NotificationListResponse> =
        ApiResponse.success(NotificationListResponse.from(useCase.list(userId, cursor, size)))

    @GetMapping("/unread-count")
    fun unreadCount(
        @CurrentUserId userId: Long,
    ): ApiResponse<NotificationCountResponse> =
        ApiResponse.success(NotificationCountResponse(useCase.unreadCount(userId)))

    @PatchMapping("/{id}/read")
    fun markRead(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
    ): ApiResponse<Nothing?> {
        useCase.markRead(userId, id)
        return ApiResponse.ok()
    }

    @PostMapping("/read-all")
    fun markAllRead(
        @CurrentUserId userId: Long,
    ): ApiResponse<NotificationCountResponse> =
        ApiResponse.success(NotificationCountResponse(useCase.markAllRead(userId).toLong()))
}
