package com.example.backend.chat.adapter.inbound.web.request

import jakarta.validation.constraints.PositiveOrZero

data class ReadChatRoomRequest(
    /** 화면에 보인 마지막 seq. */
    @field:PositiveOrZero
    val seq: Long,
)
