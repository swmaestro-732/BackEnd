package com.example.backend.notification.application.port.inbound.dto

data class NotificationSummaryPage(
    val items: List<NotificationSummary>,
    val hasNext: Boolean,
    val nextCursor: Long?,
)
