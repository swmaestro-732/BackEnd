package com.example.backend.chat.domain.model

import java.time.Instant

/** 방 안에서는 seq 로 식별한다. seq 는 순서만 보장하고 연속은 보장하지 않는다. */
data class ChatMessage(
    val roomId: Long,
    val seq: Long,
    val type: ChatMessageType,
    val senderId: Long?,
    val clientMessageId: String?,
    val body: String?,
    val status: ChatMessageStatus,
    val createdAt: Instant,
) {
    companion object {
        const val MAX_BODY_LENGTH = 1000
        const val MAX_CLIENT_MESSAGE_ID_LENGTH = 64
    }
}
