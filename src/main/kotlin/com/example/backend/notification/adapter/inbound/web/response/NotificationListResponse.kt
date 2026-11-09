package com.example.backend.notification.adapter.inbound.web.response

import com.example.backend.notification.application.port.inbound.dto.NotificationSummaryPage

data class NotificationListResponse(
    val items: List<NotificationResponse>,
    val hasNext: Boolean,
    val nextCursor: Long?,
) {
    companion object {
        fun from(page: NotificationSummaryPage): NotificationListResponse =
            NotificationListResponse(page.items.map(NotificationResponse::from), page.hasNext, page.nextCursor)
    }
}
