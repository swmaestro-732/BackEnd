package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.domain.model.ChatRoom

interface ChatRoomPersistencePort {
    fun findByAreaPrefixes(sigunguCodes: Collection<String>): List<ChatRoom>

    /** 방에 매핑된 시군구 코드. 방이 없으면 빈 목록. */
    fun findAreaPrefixes(roomId: Long): List<String>
}
