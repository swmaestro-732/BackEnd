package com.example.backend.chat.adapter.inbound.messaging

import com.example.backend.chat.application.event.ChatMemberJoinedEvent
import com.example.backend.chat.application.event.ChatMemberLeftEvent
import com.example.backend.chat.application.port.inbound.ChatMessageUseCase
import com.example.backend.chat.domain.model.ChatSystem
import com.example.backend.chat.domain.model.ChatSystemEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 멤버 변경(PostgreSQL)이 커밋된 뒤 시스템 메시지(MongoDB)를 남긴다. 두 저장소를 한 트랜잭션으로 묶을 수 없어서,
 * 시스템 메시지 저장이 실패해도 참여·나가기는 성공으로 두고 로그만 남긴다.
 */
@Component
class ChatSystemMessageListener(
    private val chatMessageUseCase: ChatMessageUseCase,
) {
    private val log = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onJoined(event: ChatMemberJoinedEvent) = send(event.roomId, ChatSystem(ChatSystemEvent.JOIN, event.userId))

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onLeft(event: ChatMemberLeftEvent) = send(event.roomId, ChatSystem(ChatSystemEvent.LEAVE, event.userId))

    private fun send(
        roomId: Long,
        system: ChatSystem,
    ) {
        try {
            chatMessageUseCase.sendSystem(roomId, system)
        } catch (e: Exception) {
            log.warn(e) { "시스템 메시지 저장 실패: roomId=$roomId event=${system.event} userId=${system.userId}" }
        }
    }
}
