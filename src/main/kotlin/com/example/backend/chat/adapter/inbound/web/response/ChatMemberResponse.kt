package com.example.backend.chat.adapter.inbound.web.response

import com.example.backend.chat.domain.model.ChatMember
import com.example.backend.chat.domain.model.ChatMemberType

data class ChatMemberResponse(
    val roomId: Long,
    val type: ChatMemberType,
) {
    companion object {
        fun from(member: ChatMember) = ChatMemberResponse(roomId = member.roomId, type = member.type)
    }
}
