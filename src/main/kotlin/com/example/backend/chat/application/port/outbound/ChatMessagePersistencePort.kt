package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.domain.model.ChatMessage

interface ChatMessagePersistencePort {
    /** 방 카운터를 원자적으로 1 올리고 그 값을 돌려준다. */
    fun nextSeq(roomId: Long): Long

    /** 같은 (roomId, senderId, clientMessageId) 가 동시에 저장되면 먼저 저장된 메시지를 돌려준다. */
    fun insertOrGet(message: ChatMessage): ChatMessage

    fun findByClientMessageId(
        roomId: Long,
        senderId: Long,
        clientMessageId: String,
    ): ChatMessage?

    /** seq 내림차순. [beforeSeq] 가 null 이면 최신부터. */
    fun findBefore(
        roomId: Long,
        beforeSeq: Long?,
        limit: Int,
    ): List<ChatMessage>

    /** seq 오름차순. */
    fun findAfter(
        roomId: Long,
        afterSeq: Long,
        limit: Int,
    ): List<ChatMessage>
}
