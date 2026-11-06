package com.example.backend.chat.adapter.inbound.web.response

import com.example.backend.chat.application.port.inbound.dto.MyChatRoomSummary
import com.example.backend.chat.domain.model.ChatMemberType
import java.time.Instant

data class MyChatRoomListResponse(
    val rooms: List<Room>,
) {
    data class Room(
        val roomId: Long,
        val name: String,
        val type: ChatMemberType,
        val joinedAt: Instant,
    )

    companion object {
        fun from(summaries: List<MyChatRoomSummary>) =
            MyChatRoomListResponse(
                summaries.map { Room(roomId = it.roomId, name = it.name, type = it.type, joinedAt = it.joinedAt) },
            )
    }
}
