package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.domain.model.ChatMessage

interface ChatMessagePersistencePort {
    /**
     * 방 카운터를 1 올리고 그 번호로 저장한다(한 트랜잭션). 같은 방은 커밋 순서가 seq 순서다.
     * 같은 (roomId, senderId, clientMessageId) 가 이미 저장돼 있으면 저장하지 않고 null 을 돌려준다(번호도 쓰지 않는다).
     */
    fun insertWithNextSeq(message: NewChatMessage): ChatMessage?

    /** 방 카운터의 마지막 seq. 메시지가 없으면 0. 이 값 이하의 메시지는 모두 저장돼 있다. */
    fun findLastSeq(roomId: Long): Long

    /** 방별 마지막 seq. 메시지가 없는 방은 결과에서 빠진다. */
    fun findLastSeqs(roomIds: Collection<Long>): Map<Long, Long>

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
