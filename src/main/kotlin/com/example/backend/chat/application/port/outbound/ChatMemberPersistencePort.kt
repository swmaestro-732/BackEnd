package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.application.port.inbound.dto.MyChatRoomSummary
import com.example.backend.chat.domain.model.ChatMember
import com.example.backend.chat.domain.model.ChatMemberType

interface ChatMemberPersistencePort {
    fun find(
        roomId: Long,
        userId: Long,
    ): ChatMember?

    fun countByUserAndType(
        userId: Long,
        type: ChatMemberType,
    ): Long

    /** 동시 요청으로 이미 행이 생겼으면 그 행을 돌려준다. */
    fun insertIfAbsent(
        roomId: Long,
        userId: Long,
        type: ChatMemberType,
    ): ChatMember

    fun delete(
        roomId: Long,
        userId: Long,
    )

    fun findRoomsByUser(userId: Long): List<MyChatRoomSummary>
}
