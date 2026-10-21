package com.example.backend.chat.adapter.outbound.persistence

import com.example.backend.chat.adapter.outbound.persistence.mongo.ChatMessageDocument
import com.example.backend.chat.adapter.outbound.persistence.mongo.ChatMessageMongoRepository
import com.example.backend.chat.application.port.outbound.ChatMessagePersistencePort
import com.example.backend.chat.domain.model.ChatMessage
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component

@Component
class ChatMessagePersistenceAdapter(
    private val chatMessageMongoRepository: ChatMessageMongoRepository,
) : ChatMessagePersistencePort {
    override fun nextSeq(roomId: Long): Long = chatMessageMongoRepository.incrementSeq(roomId)

    override fun insertOrGet(message: ChatMessage): ChatMessage =
        try {
            chatMessageMongoRepository.insert(ChatMessageDocument.from(message)).toDomain()
        } catch (e: DuplicateKeyException) {
            // 같은 clientMessageId 재전송이 동시에 들어온 경우. 받아 둔 seq 는 버려진다(seq 는 연속을 보장하지 않는다).
            val senderId = message.senderId ?: throw e
            val clientMessageId = message.clientMessageId ?: throw e
            findByClientMessageId(message.roomId, senderId, clientMessageId) ?: throw e
        }

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
