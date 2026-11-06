package com.example.backend.chat.adapter.inbound.web.response

import com.example.backend.chat.application.port.inbound.dto.ChatRoomsAtResult
import com.example.backend.chat.domain.model.ChatRoom

data class ChatRoomsAtResponse(
    val current: Room,
    val nearby: List<Room>,
) {
    data class Room(
        val roomId: Long,
        val name: String,
    )

    companion object {
        fun from(result: ChatRoomsAtResult) =
            ChatRoomsAtResponse(current = result.current.toRoom(), nearby = result.nearby.map { it.toRoom() })

        private fun ChatRoom.toRoom() = Room(roomId = id, name = name)
    }
}
