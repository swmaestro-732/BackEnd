package com.example.backend.chat.application.port.inbound.dto

import com.example.backend.chat.domain.model.ChatRoom

data class ChatRoomsAtResult(
    val current: ChatRoom,
    val nearby: List<ChatRoom>,
)
