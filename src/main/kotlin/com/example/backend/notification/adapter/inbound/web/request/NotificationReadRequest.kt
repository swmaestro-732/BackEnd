package com.example.backend.notification.adapter.inbound.web.request

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotNull

/** 알림 읽음 상태 변경. 읽지 않음으로 되돌리기는 지원하지 않아 read 는 true 만 받는다. */
data class NotificationReadRequest(
    @field:NotNull
    @field:AssertTrue(message = "read 는 true 만 지원합니다.")
    val read: Boolean? = null,
)
