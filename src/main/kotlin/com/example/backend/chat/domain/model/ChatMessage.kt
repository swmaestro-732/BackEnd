package com.example.backend.chat.domain.model

import java.time.Instant

/** 방 안에서는 seq 로 식별한다. seq 는 저장 트랜잭션으로 1부터 빠짐없이 발급된다. */
data class ChatMessage(
    val roomId: Long,
    val seq: Long,
    val type: ChatMessageType,
    val senderId: Long?,
    val clientMessageId: String?,
    val body: String?,
    val status: ChatMessageStatus,
    val createdAt: Instant,
    val card: ChatCard? = null,
    val replyTo: ChatReply? = null,
    val mentions: List<Long> = emptyList(),
) {
    companion object {
        const val MAX_BODY_LENGTH = 1000
        const val MAX_CLIENT_MESSAGE_ID_LENGTH = 64
        const val MAX_MENTIONS = 10
        const val REPLY_PREVIEW_LENGTH = 50
    }
}
