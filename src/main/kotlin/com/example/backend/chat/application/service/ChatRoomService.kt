package com.example.backend.chat.application.service

import com.example.backend.chat.application.port.inbound.ChatRoomUseCase
import com.example.backend.chat.application.port.inbound.dto.ChatRoomsAtResult
import com.example.backend.chat.application.port.inbound.dto.JoinChatRoomCommand
import com.example.backend.chat.application.port.inbound.dto.MyChatRoomSummary
import com.example.backend.chat.application.port.outbound.ChatMemberPersistencePort
import com.example.backend.chat.application.port.outbound.ChatMessagePersistencePort
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
    private val chatMessagePersistencePort: ChatMessagePersistencePort,
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
        // 들어온 시점까지는 읽은 것으로 둔다. 안 그러면 쌓여 있던 메시지가 전부 안 읽음으로 잡힌다.
        return chatMemberPersistencePort.insertIfAbsent(
            command.roomId,
            command.userId,
            type,
            lastReadSeq = chatMessagePersistencePort.findLastSeq(command.roomId),
        )
    }

    override fun leave(
        userId: Long,
        roomId: Long,
    ) = chatMemberPersistencePort.delete(roomId, userId)

    @Transactional(readOnly = true)
    override fun findMyRooms(userId: Long): List<MyChatRoomSummary> {
        val joinedRooms = chatMemberPersistencePort.findJoinedRooms(userId)
        val lastSeqs = chatMessagePersistencePort.findLastSeqs(joinedRooms.map { it.roomId })
        return joinedRooms
            .map {
                MyChatRoomSummary(
                    roomId = it.roomId,
                    name = it.name,
                    type = it.type,
                    lastReadSeq = it.lastReadSeq,
                    joinedAt = it.joinedAt,
                    unreadCount = maxOf(0, (lastSeqs[it.roomId] ?: 0) - it.lastReadSeq),
                    // 참여한 방은 많아야 몇 개라 방마다 최신 1건을 읽는다({roomId, seq} 인덱스).
                    lastMessage = chatMessagePersistencePort.findBefore(it.roomId, null, 1).firstOrNull(),
                )
            }.sortedByDescending { it.lastMessage?.createdAt ?: it.joinedAt }
    }

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
