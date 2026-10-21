package com.example.backend.chat.adapter.inbound.web.request

import com.example.backend.chat.application.port.inbound.dto.JoinChatRoomCommand
import com.example.backend.common.geo.Coordinate

data class JoinChatRoomRequest(
    val lat: Double,
    val lng: Double,
) {
    fun toCommand(
        userId: Long,
        roomId: Long,
    ) = JoinChatRoomCommand(userId = userId, roomId = roomId, coordinate = Coordinate(lat, lng))
}
