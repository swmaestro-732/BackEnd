package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.domain.model.ChatMessageStatus
import com.example.backend.chat.domain.model.ChatMessageType
import java.time.Instant

/** 저장 전 메시지. seq 는 저장할 때 발급된다. */
data class NewChatMessage(
    val roomId: Long,
    val type: ChatMessageType,
    val senderId: Long?,
    val clientMessageId: String?,
    val body: String?,
    val status: ChatMessageStatus,
    val createdAt: Instant,
)
