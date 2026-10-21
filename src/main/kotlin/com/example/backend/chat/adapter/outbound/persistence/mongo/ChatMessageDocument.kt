package com.example.backend.chat.adapter.outbound.persistence.mongo

import com.example.backend.chat.domain.model.ChatMessage
import com.example.backend.chat.domain.model.ChatMessageStatus
import com.example.backend.chat.domain.model.ChatMessageType
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/** null 필드는 저장하지 않는다(시스템 메시지는 senderId·clientMessageId 가 없다). createdAt 은 TTL 을 위해 BSON Date. */
@Document(ChatMessageDocument.COLLECTION)
data class ChatMessageDocument(
    @Id val id: ObjectId? = null,
    val roomId: Long,
    val seq: Long,
    val type: ChatMessageType,
    val senderId: Long?,
    val clientMessageId: String?,
    val body: String?,
    val status: ChatMessageStatus,
    val createdAt: Instant,
) {
    fun toDomain() =
        ChatMessage(
            roomId = roomId,
            seq = seq,
            type = type,
            senderId = senderId,
            clientMessageId = clientMessageId,
            body = body,
            status = status,
            createdAt = createdAt,
        )

    companion object {
        const val COLLECTION = "chat_messages"

        fun from(message: ChatMessage) =
            ChatMessageDocument(
                roomId = message.roomId,
                seq = message.seq,
                type = message.type,
                senderId = message.senderId,
                clientMessageId = message.clientMessageId,
                body = message.body,
                status = message.status,
                createdAt = message.createdAt,
            )
    }
}
