package com.example.backend.chat.domain.model

/** DELETED 는 작성자 삭제, HIDDEN 은 운영 조치. enum 이름이 MongoDB 저장 계약이다. */
enum class ChatMessageStatus {
    VISIBLE,
    DELETED,
    HIDDEN,
}
