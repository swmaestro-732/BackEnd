package com.example.backend.chat.application.service

import com.example.backend.chat.application.event.ChatMessageSentEvent
import com.example.backend.chat.application.port.inbound.ChatMessageUseCase
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummary
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummaryPage
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.application.port.outbound.ChatCardPort
import com.example.backend.chat.application.port.outbound.ChatMemberPersistencePort
import com.example.backend.chat.application.port.outbound.ChatMessagePersistencePort
import com.example.backend.chat.application.port.outbound.ChatSenderPort
import com.example.backend.chat.application.port.outbound.NewChatMessage
import com.example.backend.chat.domain.model.ChatMember
import com.example.backend.chat.domain.model.ChatMessage
import com.example.backend.chat.domain.model.ChatMessageStatus
import com.example.backend.chat.domain.model.ChatMessageType
import com.example.backend.chat.domain.model.ChatReply
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ChatErrorCode
import com.example.backend.common.response.CommonErrorCode
import org.springframework.context.ApplicationEventPublisher
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
    private val chatCardPort: ChatCardPort,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) : ChatMessageUseCase {
    override fun send(command: SendChatMessageCommand): ChatMessage {
        requireMember(command.roomId, command.senderId)
        chatMessagePersistencePort
            .findByClientMessageId(command.roomId, command.senderId, command.clientMessageId)
            ?.let { return it }

        val card =
            command.card?.let {
                chatCardPort.findCard(it.type, it.refId, command.senderId)
                    ?: throw BusinessException(CommonErrorCode.NOT_FOUND)
            }
        val saved =
            chatMessagePersistencePort.insertWithNextSeq(
                NewChatMessage(
                    roomId = command.roomId,
                    type = card?.type?.messageType ?: ChatMessageType.TEXT,
                    senderId = command.senderId,
                    clientMessageId = command.clientMessageId,
                    body = command.body?.takeIf { it.isNotBlank() },
                    status = ChatMessageStatus.VISIBLE,
                    createdAt = Instant.now(clock),
                    card = card,
                    replyTo = command.replyToSeq?.let { findReplyTarget(command.roomId, it) },
                    mentions = findMentions(command),
                ),
            ) ?: return findSent(command)

        // MongoDB 트랜잭션은 위에서 이미 커밋됐다. 재전송 중복은 발행하지 않는다.
        eventPublisher.publishEvent(ChatMessageSentEvent(saved))
        return saved
    }

    /** 삭제·숨김 메시지와 시스템 메시지에는 답글을 달 수 없다. */
    private fun findReplyTarget(
        roomId: Long,
        seq: Long,
    ): ChatReply {
        val original = chatMessagePersistencePort.findBySeq(roomId, seq)
        if (original == null || original.status != ChatMessageStatus.VISIBLE || original.senderId == null) {
            throw BusinessException(CommonErrorCode.NOT_FOUND)
        }
        val preview = (original.body ?: original.card?.title).orEmpty().take(ChatMessage.REPLY_PREVIEW_LENGTH)
        return ChatReply(seq = original.seq, senderId = original.senderId, preview = preview)
    }

    /** 자기 자신과 방 멤버가 아닌 사용자는 조용히 뺀다(그사이 나갔을 수 있다). */
    private fun findMentions(command: SendChatMessageCommand): List<Long> {
        val candidates = command.mentionUserIds.distinct() - command.senderId
        val members = chatMemberPersistencePort.findMemberUserIds(command.roomId, candidates)
        return candidates.filter { it in members }
    }

    /** 같은 재전송이 동시에 들어와 먼저 저장된 쪽. 중복 키는 커밋된 문서와 부딪칠 때만 나서 항상 있다. */
    private fun findSent(command: SendChatMessageCommand): ChatMessage =
        checkNotNull(
            chatMessagePersistencePort.findByClientMessageId(command.roomId, command.senderId, command.clientMessageId),
        )

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

    @Transactional
    override fun markRead(
        roomId: Long,
        userId: Long,
        seq: Long,
    ): Long {
        val member = requireMember(roomId, userId)
        val target = minOf(seq, chatMessagePersistencePort.findLastSeq(roomId))
        if (target > member.lastReadSeq) chatMemberPersistencePort.advanceLastReadSeq(roomId, userId, target)
        return maxOf(member.lastReadSeq, target)
    }

    private fun requireMember(
        roomId: Long,
        userId: Long,
    ): ChatMember =
        chatMemberPersistencePort.find(roomId, userId)
            ?: throw BusinessException(ChatErrorCode.CHAT_ROOM_NOT_JOINED)
}
