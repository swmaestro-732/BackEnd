package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.domain.model.ChatMessage

interface ChatMessagePersistencePort {
    /**
     * 방 카운터를 1 올리고 그 번호로 저장한다(한 트랜잭션). 같은 방은 커밋 순서가 seq 순서다.
     * 같은 (roomId, senderId, clientMessageId) 가 동시에 저장되면 먼저 저장된 메시지를 돌려준다.
     */
    fun insertWithNextSeq(message: NewChatMessage): ChatMessage

    /** 방 카운터의 마지막 seq. 메시지가 없으면 0. 이 값 이하의 메시지는 모두 저장돼 있다. */
    fun findLastSeq(roomId: Long): Long

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
