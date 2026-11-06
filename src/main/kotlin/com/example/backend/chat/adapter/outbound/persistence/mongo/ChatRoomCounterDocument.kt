package com.example.backend.chat.adapter.outbound.persistence.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("chat_room_counters")
data class ChatRoomCounterDocument(
    @Id val roomId: Long,
    val lastSeq: Long,
)
