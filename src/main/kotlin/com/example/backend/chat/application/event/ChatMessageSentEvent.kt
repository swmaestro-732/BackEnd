package com.example.backend.chat.application.event

import com.example.backend.chat.domain.model.ChatMessage

/** 메시지가 새로 저장됐다(재전송 중복은 발행하지 않는다). 실시간 전달·알림 등 후처리에 쓴다. */
data class ChatMessageSentEvent(
    val message: ChatMessage,
)
