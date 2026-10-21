package com.example.backend.chat.adapter.outbound.persistence

import com.example.backend.chat.adapter.outbound.persistence.exposed.repository.ChatRoomRepository
import com.example.backend.chat.application.port.outbound.ChatRoomPersistencePort
import com.example.backend.chat.domain.model.ChatRoom
import org.springframework.stereotype.Component

@Component
class ChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository,
) : ChatRoomPersistencePort {
    override fun findByAreaPrefixes(sigunguCodes: Collection<String>): List<ChatRoom> =
        if (sigunguCodes.isEmpty()) emptyList() else chatRoomRepository.findByAreaPrefixes(sigunguCodes)

    override fun findAreaPrefixes(roomId: Long): List<String> = chatRoomRepository.findAreaPrefixes(roomId)
}
