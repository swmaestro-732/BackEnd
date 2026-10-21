package com.example.backend.chat.adapter.inbound.web.request

import com.example.backend.chat.application.port.inbound.dto.ChatCardRef
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.domain.model.ChatCardType
import com.example.backend.chat.domain.model.ChatMessage
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/** 본문이나 카드 중 하나는 있어야 한다. 카드에 붙인 본문은 한마디 코멘트로 쓴다. */
data class SendChatMessageRequest(
    /** 클라이언트가 만든 UUID. 재전송해도 같은 값을 보내야 중복 저장되지 않는다. */
    @field:NotBlank
    @field:Size(max = ChatMessage.MAX_CLIENT_MESSAGE_ID_LENGTH)
    val clientMessageId: String,
    @field:Size(max = ChatMessage.MAX_BODY_LENGTH)
    val body: String? = null,
    @field:Valid
    val card: Card? = null,
    @field:Positive
    val replyToSeq: Long? = null,
    /** 방 멤버가 아니거나 자기 자신이면 서버가 뺀다. */
    @field:Size(max = ChatMessage.MAX_MENTIONS)
    val mentionUserIds: List<Long> = emptyList(),
) {
    @get:AssertTrue(message = "본문이나 카드 중 하나는 있어야 합니다.")
    val contentPresent: Boolean
        get() = card != null || !body.isNullOrBlank()

    data class Card(
        val type: ChatCardType,
        @field:Positive
        val refId: Long,
    )

    fun toCommand(
        roomId: Long,
        senderId: Long,
    ) = SendChatMessageCommand(
        roomId = roomId,
        senderId = senderId,
        clientMessageId = clientMessageId,
        body = body,
        card = card?.let { ChatCardRef(it.type, it.refId) },
        replyToSeq = replyToSeq,
        mentionUserIds = mentionUserIds,
    )
}
