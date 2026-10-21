package com.example.backend.chat.adapter.outbound.persistence

import com.example.backend.chat.adapter.outbound.persistence.mongo.ChatMessageMongoRepository
import com.example.backend.chat.application.port.outbound.ChatMessagePersistencePort
import com.example.backend.chat.application.port.outbound.NewChatMessage
import com.example.backend.chat.domain.model.ChatMessage
import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import org.springframework.stereotype.Component

@Component
class ChatMessagePersistenceAdapter(
    private val chatMessageMongoRepository: ChatMessageMongoRepository,
) : ChatMessagePersistencePort {
    override fun insertWithNextSeq(message: NewChatMessage): ChatMessage =
        try {
            chatMessageMongoRepository.insertWithNextSeq(message).toDomain()
        } catch (e: MongoWriteException) {
            // 같은 clientMessageId 재전송이 동시에 들어온 경우. 트랜잭션이 중단돼 카운터도 되돌아간다.
            if (e.error.category != ErrorCategory.DUPLICATE_KEY) throw e
            val senderId = message.senderId ?: throw e
            val clientMessageId = message.clientMessageId ?: throw e
            findByClientMessageId(message.roomId, senderId, clientMessageId) ?: throw e
        }

    override fun findLastSeq(roomId: Long): Long = chatMessageMongoRepository.findLastSeq(roomId)

    override fun findByClientMessageId(
        roomId: Long,
        senderId: Long,
        clientMessageId: String,
    ): ChatMessage? = chatMessageMongoRepository.findByClientMessageId(roomId, senderId, clientMessageId)?.toDomain()

    override fun findBefore(
        roomId: Long,
        beforeSeq: Long?,
        limit: Int,
    ): List<ChatMessage> = chatMessageMongoRepository.findBefore(roomId, beforeSeq, limit).map { it.toDomain() }

    override fun findAfter(
        roomId: Long,
        afterSeq: Long,
        limit: Int,
    ): List<ChatMessage> = chatMessageMongoRepository.findAfter(roomId, afterSeq, limit).map { it.toDomain() }
}
