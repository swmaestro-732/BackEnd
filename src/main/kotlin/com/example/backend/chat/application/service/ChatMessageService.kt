package com.example.backend.chat.application.service

import com.example.backend.chat.application.port.inbound.ChatMessageUseCase
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummary
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummaryPage
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.application.port.outbound.ChatMemberPersistencePort
import com.example.backend.chat.application.port.outbound.ChatMessagePersistencePort
import com.example.backend.chat.application.port.outbound.ChatSenderPort
import com.example.backend.chat.application.port.outbound.NewChatMessage
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

        return chatMessagePersistencePort.insertWithNextSeq(
            NewChatMessage(
                roomId = command.roomId,
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

        // 메시지 조회보다 먼저 읽어야 한다. 이 값 이하는 모두 저장돼 있다(트랜잭션 발급).
        // beforeSeq(위로 스크롤)는 동기화 범위와 관계없어 읽지 않는다.
        val lastSeq = if (beforeSeq == null) chatMessagePersistencePort.findLastSeq(roomId) else null

        // 한 건 더 읽어 다음 페이지가 있는지 판단한다.
        val fetched =
            if (afterSeq != null) {
                chatMessagePersistencePort.findAfter(roomId, afterSeq, size + 1)
            } else {
                chatMessagePersistencePort.findBefore(roomId, beforeSeq, size + 1)
            }
        val hasMore = fetched.size > size
        val page = fetched.take(size).let { if (afterSeq != null) it else it.reversed() }

        val throughSeq =
            when {
                lastSeq == null -> null

                // 놓친 메시지가 더 남았으면 이번에 확인한 곳까지만.
                afterSeq != null && hasMore -> page.last().seq

                // 조회 사이에 새로 저장된 메시지가 섞여 들어왔을 수 있어 둘 중 큰 값.
                else -> maxOf(lastSeq, page.lastOrNull()?.seq ?: 0)
            }

        val senders = chatSenderPort.findSenders(page.mapNotNull { it.senderId }.toSet()).associateBy { it.userId }
        return ChatMessageSummaryPage(
            messages = page.map { ChatMessageSummary(message = it, sender = it.senderId?.let(senders::get)) },
            hasMore = hasMore,
            throughSeq = throughSeq,
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
