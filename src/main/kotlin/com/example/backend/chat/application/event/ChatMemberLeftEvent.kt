package com.example.backend.chat.application.event

/** 방에서 나갔다. 커밋 후 퇴장 시스템 메시지를 남긴다. */
data class ChatMemberLeftEvent(
    val roomId: Long,
    val userId: Long,
)
