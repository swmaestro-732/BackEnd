package com.example.backend.chat.adapter.inbound.web.response

import com.example.backend.chat.application.port.inbound.dto.MyChatRoomSummary
import com.example.backend.chat.domain.model.ChatMemberType
import com.example.backend.chat.domain.model.ChatMessage
import com.example.backend.chat.domain.model.ChatMessageStatus
import com.example.backend.chat.domain.model.ChatMessageType
import com.example.backend.chat.domain.model.ChatSystemEvent
import java.time.Instant

data class MyChatRoomListResponse(
    val rooms: List<Room>,
) {
    data class Room(
        val roomId: Long,
        val name: String,
        val type: ChatMemberType,
        val joinedAt: Instant,
        val unreadCount: Long,
        val lastMessage: LastMessage?,
    )

    /** 삭제·숨김 메시지는 body·card 를 내리지 않는다. */
    data class LastMessage(
        val seq: Long,
        val type: ChatMessageType,
        val body: String?,
        val card: ChatCardResponse?,
        val system: System?,
        val status: ChatMessageStatus,
        val createdAt: Instant,
    )

    /** 닉네임은 내려주지 않는다. 미리보기에 필요하면 [userId] 로 조회한다. */
    data class System(
        val event: ChatSystemEvent,
        val userId: Long,
    )

    companion object {
        fun from(summaries: List<MyChatRoomSummary>) =
            MyChatRoomListResponse(
                summaries.map {
                    Room(
                        roomId = it.roomId,
                        name = it.name,
                        type = it.type,
                        joinedAt = it.joinedAt,
                        unreadCount = it.unreadCount,
                        lastMessage = it.lastMessage?.toLastMessage(),
                    )
                },
            )

        private fun ChatMessage.toLastMessage() =
            LastMessage(
                seq = seq,
                type = type,
                body = body.takeIf { status == ChatMessageStatus.VISIBLE },
                card = card?.takeIf { status == ChatMessageStatus.VISIBLE }?.let(ChatCardResponse::from),
                system = system?.let { System(it.event, it.userId) },
                status = status,
                createdAt = createdAt,
            )
    }
}
