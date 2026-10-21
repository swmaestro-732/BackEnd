package com.example.backend.chat.adapter.inbound.web.response

import com.example.backend.chat.domain.model.ChatMessage
import java.time.Instant

data class SentChatMessageResponse(
    val seq: Long,
    val clientMessageId: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(message: ChatMessage) =
            SentChatMessageResponse(
                seq = message.seq,
                clientMessageId = message.clientMessageId,
                createdAt = message.createdAt,
            )
    }
}
