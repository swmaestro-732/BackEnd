package com.example.backend.chat.adapter.outbound.persistence.exposed.repository

import com.example.backend.chat.adapter.outbound.persistence.exposed.ChatMemberTable
import com.example.backend.chat.adapter.outbound.persistence.exposed.ChatRoomTable
import com.example.backend.chat.application.port.inbound.dto.MyChatRoomSummary
import com.example.backend.chat.domain.model.ChatMember
import com.example.backend.chat.domain.model.ChatMemberType
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import kotlin.time.toJavaInstant

@Repository
class ChatMemberRepository {
    fun find(
        roomId: Long,
        userId: Long,
    ): ChatMember? =
        ChatMemberTable
            .selectAll()
            .where { (ChatMemberTable.roomId eq roomId) and (ChatMemberTable.userId eq userId) }
            .firstOrNull()
            ?.toDomain()

    fun countByUserAndType(
        userId: Long,
        type: ChatMemberType,
    ): Long =
        ChatMemberTable
            .selectAll()
            .where { (ChatMemberTable.userId eq userId) and (ChatMemberTable.type eq type) }
            .count()

    /** ON CONFLICT DO NOTHING — 동시 참여 요청이 겹쳐도 PK 충돌(500) 없이 한 행만 남는다. */
    fun insertIgnore(
        roomId: Long,
        userId: Long,
        type: ChatMemberType,
    ) {
        ChatMemberTable.insertIgnore {
            it[ChatMemberTable.roomId] = roomId
            it[ChatMemberTable.userId] = userId
            it[ChatMemberTable.type] = type
        }
    }

    fun delete(
        roomId: Long,
        userId: Long,
    ) {
        ChatMemberTable.deleteWhere {
            (ChatMemberTable.roomId eq roomId) and (ChatMemberTable.userId eq userId)
        }
    }

    fun findRoomsByUser(userId: Long): List<MyChatRoomSummary> =
        ChatMemberTable
            .join(ChatRoomTable, JoinType.INNER, ChatMemberTable.roomId, ChatRoomTable.id)
            .selectAll()
            .where { ChatMemberTable.userId eq userId }
            .orderBy(ChatMemberTable.joinedAt to SortOrder.DESC)
            .map {
                MyChatRoomSummary(
                    roomId = it[ChatRoomTable.id].value,
                    name = it[ChatRoomTable.name],
                    type = it[ChatMemberTable.type],
                    lastReadSeq = it[ChatMemberTable.lastReadSeq],
                    joinedAt = it[ChatMemberTable.joinedAt].toJavaInstant(),
                )
            }

    private fun ResultRow.toDomain() =
        ChatMember(
            roomId = this[ChatMemberTable.roomId],
            userId = this[ChatMemberTable.userId],
            type = this[ChatMemberTable.type],
            lastReadSeq = this[ChatMemberTable.lastReadSeq],
            joinedAt = this[ChatMemberTable.joinedAt].toJavaInstant(),
        )
}
