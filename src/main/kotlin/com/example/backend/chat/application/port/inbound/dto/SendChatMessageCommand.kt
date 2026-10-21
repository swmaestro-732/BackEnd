package com.example.backend.chat.application.port.inbound.dto

data class SendChatMessageCommand(
    val roomId: Long,
    val senderId: Long,
    val clientMessageId: String,
    val body: String?,
    val card: ChatCardRef? = null,
    val replyToSeq: Long? = null,
    val mentionUserIds: List<Long> = emptyList(),
) {
    init {
        require(card != null || !body.isNullOrBlank()) { "본문이나 카드 중 하나는 있어야 합니다." }
    }
}
