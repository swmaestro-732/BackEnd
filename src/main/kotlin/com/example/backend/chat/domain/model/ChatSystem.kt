package com.example.backend.chat.domain.model

/** 시스템 메시지 내용. 문장은 저장하지 않고 이벤트만 저장해 문구는 클라이언트가 만든다. */
data class ChatSystem(
    val event: ChatSystemEvent,
    val userId: Long,
)
