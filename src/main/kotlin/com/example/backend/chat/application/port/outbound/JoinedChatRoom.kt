package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.domain.model.ChatMemberType
import java.time.Instant

data class JoinedChatRoom(
    val roomId: Long,
    val name: String,
    val type: ChatMemberType,
    val lastReadSeq: Long,
    val joinedAt: Instant,
)
