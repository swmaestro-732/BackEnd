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

    /** 새로 넣었으면 true. 동시 요청으로 이미 행이 생겼으면 아무것도 하지 않고 false. */
    fun insertIfAbsent(
        roomId: Long,
        userId: Long,
        type: ChatMemberType,
        lastReadSeq: Long,
    ): Boolean

    /** 지웠으면 true. 이미 없었으면 false. */
    fun delete(
        roomId: Long,
        userId: Long,
    ): Boolean

    fun findJoinedRooms(userId: Long): List<JoinedChatRoom>

    /** 읽음 번호를 [seq] 로 올린다. 이미 같거나 크면 바꾸지 않는다. */
    fun advanceLastReadSeq(
        roomId: Long,
        userId: Long,
        seq: Long,
    )
}
