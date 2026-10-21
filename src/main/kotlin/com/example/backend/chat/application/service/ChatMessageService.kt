package com.example.backend.chat.application.service

import com.example.backend.chat.application.port.inbound.ChatMessageUseCase
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummary
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummaryPage
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.application.port.outbound.ChatMemberPersistencePort
import com.example.backend.chat.application.port.outbound.ChatMessagePersistencePort
import com.example.backend.chat.application.port.outbound.ChatSenderPort
import com.example.backend.chat.domain.model.ChatMessage
import com.example.backend.chat.domain.model.ChatMessageStatus
import com.example.backend.chat.domain.model.ChatMessageType
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ChatErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/** 메시지는 MongoDB 에 저장한다. 트랜잭션은 PostgreSQL 멤버 조회에만 걸린다. */
@Service
@Transactional(readOnly = true)
class ChatMessageService(
    private val chatMemberPersistencePort: ChatMemberPersistencePort,
    private val chatMessagePersistencePort: ChatMessagePersistencePort,
    private val chatSenderPort: ChatSenderPort,
    private val clock: Clock,
) : ChatMessageUseCase {
    override fun send(command: SendChatMessageCommand): ChatMessage {
        requireMember(command.roomId, command.senderId)
        chatMessagePersistencePort
            .findByClientMessageId(command.roomId, command.senderId, command.clientMessageId)
            ?.let { return it }

        return chatMessagePersistencePort.insertOrGet(
            ChatMessage(
                roomId = command.roomId,
                seq = chatMessagePersistencePort.nextSeq(command.roomId),
                type = ChatMessageType.TEXT,
                senderId = command.senderId,
                clientMessageId = command.clientMessageId,
                body = command.body,
                status = ChatMessageStatus.VISIBLE,
                createdAt = Instant.now(clock),
            ),
        )
    }

    override fun list(
        roomId: Long,
        userId: Long,
        beforeSeq: Long?,
        afterSeq: Long?,
        size: Int,
    ): ChatMessageSummaryPage {
        require(beforeSeq == null || afterSeq == null) { "beforeSeq 와 afterSeq 는 함께 쓸 수 없습니다." }
        requireMember(roomId, userId)

        // 한 건 더 읽어 다음 페이지가 있는지 판단한다.
        val fetched =
            if (afterSeq != null) {
                chatMessagePersistencePort.findAfter(roomId, afterSeq, size + 1)
            } else {
                chatMessagePersistencePort.findBefore(roomId, beforeSeq, size + 1)
            }
        val page = fetched.take(size).let { if (afterSeq != null) it else it.reversed() }

        val senders = chatSenderPort.findSenders(page.mapNotNull { it.senderId }.toSet()).associateBy { it.userId }
        return ChatMessageSummaryPage(
            messages = page.map { ChatMessageSummary(message = it, sender = it.senderId?.let(senders::get)) },
            hasMore = fetched.size > size,
        )
    }

    private fun requireMember(
        roomId: Long,
        userId: Long,
    ) {
        chatMemberPersistencePort.find(roomId, userId)
            ?: throw BusinessException(ChatErrorCode.CHAT_ROOM_NOT_JOINED)
    }
}
