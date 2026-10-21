package com.example.backend.chat.adapter.outbound.persistence

import com.example.backend.chat.adapter.outbound.persistence.exposed.repository.ChatMemberRepository
import com.example.backend.chat.application.port.outbound.ChatMemberPersistencePort
import com.example.backend.chat.application.port.outbound.JoinedChatRoom
import com.example.backend.chat.domain.model.ChatMember
import com.example.backend.chat.domain.model.ChatMemberType
import org.springframework.stereotype.Component

@Component
class ChatMemberPersistenceAdapter(
    private val chatMemberRepository: ChatMemberRepository,
) : ChatMemberPersistencePort {
    override fun find(
        roomId: Long,
        userId: Long,
    ): ChatMember? = chatMemberRepository.find(roomId, userId)

    override fun findMemberUserIds(
        roomId: Long,
        userIds: Collection<Long>,
    ): Set<Long> = if (userIds.isEmpty()) emptySet() else chatMemberRepository.findMemberUserIds(roomId, userIds)

    override fun countByUserAndType(
        userId: Long,
        type: ChatMemberType,
    ): Long = chatMemberRepository.countByUserAndType(userId, type)

    override fun insertIfAbsent(
        roomId: Long,
        userId: Long,
        type: ChatMemberType,
        lastReadSeq: Long,
    ): Boolean = chatMemberRepository.insertIgnore(roomId, userId, type, lastReadSeq) > 0

    override fun delete(
        roomId: Long,
        userId: Long,
    ): Boolean = chatMemberRepository.delete(roomId, userId) > 0

    override fun findJoinedRooms(userId: Long): List<JoinedChatRoom> = chatMemberRepository.findJoinedRooms(userId)

    override fun advanceLastReadSeq(
        roomId: Long,
        userId: Long,
        seq: Long,
    ) = chatMemberRepository.advanceLastReadSeq(roomId, userId, seq)
}
