package com.example.backend.chat.application.service

import com.example.backend.chat.application.port.inbound.ChatRoomUseCase
import com.example.backend.chat.application.port.inbound.dto.ChatRoomsAtResult
import com.example.backend.chat.application.port.inbound.dto.JoinChatRoomCommand
import com.example.backend.chat.application.port.inbound.dto.MyChatRoomSummary
import com.example.backend.chat.application.port.outbound.ChatMemberPersistencePort
import com.example.backend.chat.application.port.outbound.ChatRegionPort
import com.example.backend.chat.application.port.outbound.ChatRoomPersistencePort
import com.example.backend.chat.domain.model.ChatMember
import com.example.backend.chat.domain.model.ChatMemberType
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.ChatErrorCode
import com.example.backend.common.response.CommonErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ChatRoomService(
    private val chatRegionPort: ChatRegionPort,
    private val chatRoomPersistencePort: ChatRoomPersistencePort,
    private val chatMemberPersistencePort: ChatMemberPersistencePort,
) : ChatRoomUseCase {
    @Transactional(readOnly = true)
    override fun findRoomsAt(coordinate: Coordinate): ChatRoomsAtResult {
        val sigunguCode = sigunguCodeAt(coordinate)
        val current =
            chatRoomPersistencePort.findByAreaPrefixes(listOf(sigunguCode)).firstOrNull()
                ?: throw BusinessException(ChatErrorCode.CHAT_REGION_UNSUPPORTED)
        val nearby =
            chatRoomPersistencePort
                .findByAreaPrefixes(chatRegionPort.findAdjacentSigunguCodes(sigunguCode))
                .filter { it.id != current.id }
        return ChatRoomsAtResult(current = current, nearby = nearby)
    }

    override fun join(command: JoinChatRoomCommand): ChatMember {
        chatMemberPersistencePort.find(command.roomId, command.userId)?.let { return it }

        val roomCodes = chatRoomPersistencePort.findAreaPrefixes(command.roomId)
        if (roomCodes.isEmpty()) throw BusinessException(CommonErrorCode.NOT_FOUND)

        val type = memberTypeOf(roomCodes, sigunguCodeAt(command.coordinate))
        if (type == ChatMemberType.NEARBY &&
            chatMemberPersistencePort.countByUserAndType(command.userId, ChatMemberType.NEARBY) >=
            ChatMember.MAX_NEARBY_ROOMS
        ) {
            throw BusinessException(ChatErrorCode.CHAT_NEARBY_ROOM_LIMIT_EXCEEDED)
        }
        return chatMemberPersistencePort.insertIfAbsent(command.roomId, command.userId, type)
    }

    override fun leave(
        userId: Long,
        roomId: Long,
    ) = chatMemberPersistencePort.delete(roomId, userId)

    @Transactional(readOnly = true)
    override fun findMyRooms(userId: Long): List<MyChatRoomSummary> = chatMemberPersistencePort.findRoomsByUser(userId)

    private fun sigunguCodeAt(coordinate: Coordinate): String =
        chatRegionPort.findSigunguCodeAt(coordinate)
            ?: throw BusinessException(ChatErrorCode.CHAT_REGION_UNSUPPORTED)

    private fun memberTypeOf(
        roomCodes: List<String>,
        sigunguCode: String,
    ): ChatMemberType =
        when {
            sigunguCode in roomCodes -> ChatMemberType.CURRENT
            chatRegionPort.findAdjacentSigunguCodes(sigunguCode).any { it in roomCodes } -> ChatMemberType.NEARBY
            else -> throw BusinessException(ChatErrorCode.CHAT_ROOM_OUT_OF_RANGE)
        }
}
