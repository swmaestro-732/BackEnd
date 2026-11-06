package com.example.backend.chat.adapter.outbound.persistence.exposed.repository

import com.example.backend.chat.adapter.outbound.persistence.exposed.ChatRoomRegionTable
import com.example.backend.chat.adapter.outbound.persistence.exposed.ChatRoomTable
import com.example.backend.chat.domain.model.ChatRegionType
import com.example.backend.chat.domain.model.ChatRoom
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

@Repository
class ChatRoomRepository {
    fun findByAreaPrefixes(sigunguCodes: Collection<String>): List<ChatRoom> =
        ChatRoomRegionTable
            .join(ChatRoomTable, JoinType.INNER, ChatRoomRegionTable.roomId, ChatRoomTable.id)
            .select(ChatRoomTable.id, ChatRoomTable.name)
            .where {
                (ChatRoomRegionTable.regionType eq ChatRegionType.AREA_PREFIX) and
                    (ChatRoomRegionTable.regionValue inList sigunguCodes)
            }.map { ChatRoom(id = it[ChatRoomTable.id].value, name = it[ChatRoomTable.name]) }
            // 여러 구를 묶은 방은 행이 여러 개 나온다.
            .distinctBy { it.id }

    fun findAreaPrefixes(roomId: Long): List<String> =
        ChatRoomRegionTable
            .select(ChatRoomRegionTable.regionValue)
            .where {
                (ChatRoomRegionTable.roomId eq roomId) and
                    (ChatRoomRegionTable.regionType eq ChatRegionType.AREA_PREFIX)
            }.map { it[ChatRoomRegionTable.regionValue] }
}
