package com.example.backend.chat.adapter.inbound.messaging

import com.example.backend.chat.application.event.ChatMemberJoinedEvent
import com.example.backend.chat.application.event.ChatMemberLeftEvent
import com.example.backend.chat.application.port.inbound.ChatMessageUseCase
import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummaryPage
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.domain.model.ChatMessage
import com.example.backend.chat.domain.model.ChatSystem
import com.example.backend.chat.domain.model.ChatSystemEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ChatSystemMessageListenerTest {
    private val requested = mutableListOf<ChatSystem>()

    private val failingUseCase =
        object : ChatMessageUseCase {
            override fun send(command: SendChatMessageCommand): ChatMessage = error("사용하지 않음")

            override fun sendSystem(
                roomId: Long,
                system: ChatSystem,
            ): ChatMessage {
                requested += system
                throw IllegalStateException("MongoDB 연결 실패")
            }

            override fun list(
                roomId: Long,
                userId: Long,
                beforeSeq: Long?,
                afterSeq: Long?,
                size: Int,
            ): ChatMessageSummaryPage = error("사용하지 않음")

            override fun markRead(
                roomId: Long,
                userId: Long,
                seq: Long,
            ): Long = error("사용하지 않음")
        }

    @Test
    fun `시스템 메시지 저장이 실패해도 예외를 밖으로 던지지 않는다`() {
        val listener = ChatSystemMessageListener(failingUseCase)

        assertDoesNotThrow {
            listener.onJoined(ChatMemberJoinedEvent(roomId = 1, userId = 7))
            listener.onLeft(ChatMemberLeftEvent(roomId = 1, userId = 7))
        }
        assertEquals(listOf(ChatSystemEvent.JOIN, ChatSystemEvent.LEAVE), requested.map { it.event })
    }
}
