package com.example.backend.chat.application.port.inbound.dto

import com.example.backend.chat.domain.model.ChatCardType

data class ChatCardRef(
    val type: ChatCardType,
    val refId: Long,
)
