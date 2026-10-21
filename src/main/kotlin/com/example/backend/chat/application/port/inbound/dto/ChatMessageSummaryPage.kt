package com.example.backend.chat.application.port.inbound.dto

import com.example.backend.chat.domain.model.ChatMessage

data class ChatMessageSummaryPage(
    val messages: List<ChatMessageSummary>,
    val hasMore: Boolean,
)

/** [sender] 가 null 이면 시스템 메시지이거나 탈퇴한 사용자다. */
data class ChatMessageSummary(
    val message: ChatMessage,
    val sender: ChatSender?,
)

data class ChatSender(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
