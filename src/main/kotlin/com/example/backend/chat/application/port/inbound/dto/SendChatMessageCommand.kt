package com.example.backend.chat.application.port.inbound.dto

data class SendChatMessageCommand(
    val roomId: Long,
    val senderId: Long,
    val clientMessageId: String,
    val body: String,
)
