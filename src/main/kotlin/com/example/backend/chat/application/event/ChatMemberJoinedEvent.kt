package com.example.backend.chat.application.event

/** 방에 새로 참여했다. 커밋 후 입장 시스템 메시지를 남긴다. */
data class ChatMemberJoinedEvent(
    val roomId: Long,
    val userId: Long,
)
