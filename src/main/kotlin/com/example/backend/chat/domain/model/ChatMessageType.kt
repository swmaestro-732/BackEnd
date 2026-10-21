package com.example.backend.chat.domain.model

/** enum 이름이 MongoDB 저장 계약이다. */
enum class ChatMessageType {
    TEXT,
    PLACE,
    COURSE,
    SYSTEM,
}
