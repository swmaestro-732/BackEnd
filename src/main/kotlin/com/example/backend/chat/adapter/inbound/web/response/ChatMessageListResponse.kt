package com.example.backend.chat.adapter.inbound.web.response

import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummary
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummaryPage
import com.example.backend.chat.domain.model.ChatMessageStatus
import com.example.backend.chat.domain.model.ChatMessageType
import java.time.Instant

data class ChatMessageListResponse(
    val messages: List<Message>,
    val hasMore: Boolean,
    /** 여기까지 빠짐없이 확인함. 클라이언트는 syncedSeq = max(syncedSeq, throughSeq). beforeSeq 조회면 null. */
    val throughSeq: Long?,
) {
    /** [sender] 가 null 이면 시스템 메시지이거나 탈퇴한 사용자다. 삭제·숨김 메시지는 내용(body·card·replyTo·mentions)을 내리지 않는다. */
    data class Message(
        val seq: Long,
        val type: ChatMessageType,
        val sender: Sender?,
        val clientMessageId: String?,
        val body: String?,
        val card: ChatCardResponse?,
        val replyTo: Reply?,
        val mentions: List<Long>,
        val status: ChatMessageStatus,
        val createdAt: Instant,
    )

    /** 답글 대상 원문 스냅샷. 작성자 정보는 [senderId] 로 붙인다. */
    data class Reply(
        val seq: Long,
        val senderId: Long,
        val preview: String,
    )

    data class Sender(
        val userId: Long,
        val nickname: String,
        val profileImageUrl: String?,
    )

    companion object {
        fun from(page: ChatMessageSummaryPage) =
            ChatMessageListResponse(
                messages = page.messages.map { it.toMessage() },
                hasMore = page.hasMore,
                throughSeq = page.throughSeq,
            )

        private fun ChatMessageSummary.toMessage(): Message {
            val visible = message.status == ChatMessageStatus.VISIBLE
            return Message(
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
                body = message.body.takeIf { visible },
                card = message.card?.takeIf { visible }?.let(ChatCardResponse::from),
                replyTo = message.replyTo?.takeIf { visible }?.let { Reply(it.seq, it.senderId, it.preview) },
                mentions = if (visible) message.mentions else emptyList(),
                status = message.status,
                createdAt = message.createdAt,
            )
        }
    }
}
