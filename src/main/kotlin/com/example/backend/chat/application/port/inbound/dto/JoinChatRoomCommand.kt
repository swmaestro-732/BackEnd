package com.example.backend.chat.application.port.inbound.dto

import com.example.backend.common.geo.Coordinate

data class JoinChatRoomCommand(
    val userId: Long,
    val roomId: Long,
    val coordinate: Coordinate,
)
