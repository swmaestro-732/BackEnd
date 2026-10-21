package com.example.backend.chat.adapter.inbound.web.response

import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummary
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummaryPage
import com.example.backend.chat.domain.model.ChatMessageStatus
import com.example.backend.chat.domain.model.ChatMessageType
import java.time.Instant

data class ChatMessageListResponse(
    val messages: List<Message>,
    val hasMore: Boolean,
) {
    /** [sender] 가 null 이면 시스템 메시지이거나 탈퇴한 사용자다. 삭제·숨김 메시지는 body 를 내리지 않는다. */
    data class Message(
        val seq: Long,
        val type: ChatMessageType,
        val sender: Sender?,
        val clientMessageId: String?,
        val body: String?,
        val status: ChatMessageStatus,
        val createdAt: Instant,
    )

    data class Sender(
        val userId: Long,
        val nickname: String,
        val profileImageUrl: String?,
    )

    companion object {
        fun from(page: ChatMessageSummaryPage) =
            ChatMessageListResponse(messages = page.messages.map { it.toMessage() }, hasMore = page.hasMore)

        private fun ChatMessageSummary.toMessage() =
            Message(
                seq = message.seq,
                type = message.type,
                sender =
                    sender?.let {
                        Sender(
                            userId = it.userId,
                            nickname = it.nickname,
                            profileImageUrl = it.profileImageUrl,
                        )
                    },
                clientMessageId = message.clientMessageId,
                body = message.body.takeIf { message.status == ChatMessageStatus.VISIBLE },
                status = message.status,
                createdAt = message.createdAt,
            )
    }
}
