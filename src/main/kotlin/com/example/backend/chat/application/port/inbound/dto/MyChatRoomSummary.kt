package com.example.backend.chat.application.port.inbound.dto

import com.example.backend.chat.domain.model.ChatMemberType
import java.time.Instant

data class MyChatRoomSummary(
    val roomId: Long,
    val name: String,
    val type: ChatMemberType,
    val lastReadSeq: Long,
    val joinedAt: Instant,
)
