package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.domain.model.ChatMember
import com.example.backend.chat.domain.model.ChatMemberType

interface ChatMemberPersistencePort {
    fun find(
        roomId: Long,
        userId: Long,
    ): ChatMember?

    /** [userIds] 중 이 방 멤버인 사용자. */
    fun findMemberUserIds(
        roomId: Long,
        userIds: Collection<Long>,
    ): Set<Long>

    fun countByUserAndType(
        userId: Long,
        type: ChatMemberType,
    ): Long

    /** 동시 요청으로 이미 행이 생겼으면 그 행을 돌려준다. */
    fun insertIfAbsent(
        roomId: Long,
        userId: Long,
        type: ChatMemberType,
        lastReadSeq: Long,
    ): ChatMember

    fun delete(
        roomId: Long,
        userId: Long,
    )

    fun findJoinedRooms(userId: Long): List<JoinedChatRoom>

    /** 읽음 번호를 [seq] 로 올린다. 이미 같거나 크면 바꾸지 않는다. */
    fun advanceLastReadSeq(
        roomId: Long,
        userId: Long,
        seq: Long,
    )
}
