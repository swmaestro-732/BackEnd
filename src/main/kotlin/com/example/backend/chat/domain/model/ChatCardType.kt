package com.example.backend.chat.domain.model

/** enum 이름이 MongoDB 저장 계약이다. */
enum class ChatCardType(
    val messageType: ChatMessageType,
) {
    PLACE(ChatMessageType.PLACE),
    COURSE(ChatMessageType.COURSE),
}
