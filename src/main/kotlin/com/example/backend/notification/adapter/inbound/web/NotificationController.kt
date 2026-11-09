package com.example.backend.notification.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.notification.adapter.inbound.web.request.NotificationReadRequest
import com.example.backend.notification.adapter.inbound.web.response.NotificationCountResponse
import com.example.backend.notification.adapter.inbound.web.response.NotificationListResponse
import com.example.backend.notification.application.port.inbound.NotificationUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/my/notifications")
@AccessTokenRequired
@RequiresAppFeature(AppFeature.NOTIFICATION)
class NotificationController(
    private val notificationUseCase: NotificationUseCase,
) {
    @GetMapping
    fun list(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int = 20,
    ): ApiResponse<NotificationListResponse> =
        ApiResponse.success(NotificationListResponse.from(notificationUseCase.list(userId, cursor, size)))

    @GetMapping("/unread-count")
    fun unreadCount(
        @CurrentUserId userId: Long,
    ): ApiResponse<NotificationCountResponse> =
        ApiResponse.success(NotificationCountResponse(notificationUseCase.unreadCount(userId)))

    /** 알림 하나를 읽음으로 바꾼다. 남의 알림이나 없는 id 도 200(멱등). */
    @PatchMapping("/{id}")
    fun markRead(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: NotificationReadRequest,
    ): ApiResponse<Nothing?> {
        notificationUseCase.markRead(userId, id)
        return ApiResponse.ok()
    }

    /** 내 알림 전체를 읽음으로 바꾸고 바뀐 개수를 돌려준다. */
    @PatchMapping
    fun markAllRead(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: NotificationReadRequest,
    ): ApiResponse<NotificationCountResponse> =
        ApiResponse.success(NotificationCountResponse(notificationUseCase.markAllRead(userId).toLong()))
}
