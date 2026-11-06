package com.example.backend.chat.adapter.inbound.web.request

import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.domain.model.ChatMessage
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SendChatMessageRequest(
    /** 클라이언트가 만든 UUID. 재전송해도 같은 값을 보내야 중복 저장되지 않는다. */
    @field:NotBlank
    @field:Size(max = ChatMessage.MAX_CLIENT_MESSAGE_ID_LENGTH)
    val clientMessageId: String,
    @field:NotBlank
    @field:Size(max = ChatMessage.MAX_BODY_LENGTH)
    val body: String,
) {
    fun toCommand(
        roomId: Long,
        senderId: Long,
    ) = SendChatMessageCommand(roomId = roomId, senderId = senderId, clientMessageId = clientMessageId, body = body)
}
