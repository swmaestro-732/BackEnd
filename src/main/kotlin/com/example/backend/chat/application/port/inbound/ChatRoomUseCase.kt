package com.example.backend.chat.application.port.inbound

import com.example.backend.chat.application.port.inbound.dto.ChatRoomsAtResult
import com.example.backend.chat.application.port.inbound.dto.JoinChatRoomCommand
import com.example.backend.chat.application.port.inbound.dto.MyChatRoomSummary
import com.example.backend.chat.domain.model.ChatMember
import com.example.backend.common.geo.Coordinate

interface ChatRoomUseCase {
    /** 현재 위치의 방과 인접 구의 방. 지원하지 않는 지역이면 4007. */
    fun findRoomsAt(coordinate: Coordinate): ChatRoomsAtResult

    /** 위치로 참여 유형을 정한다. 이미 참여 중이면 기존 멤버를 돌려준다. 방이 없으면 4040, 범위 밖이면 4032, 인접 방 한도 초과면 4008. */
    fun join(command: JoinChatRoomCommand): ChatMember

    fun leave(
        userId: Long,
        roomId: Long,
    )

    fun findMyRooms(userId: Long): List<MyChatRoomSummary>
}
