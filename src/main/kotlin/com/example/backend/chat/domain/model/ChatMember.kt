package com.example.backend.chat.domain.model

import java.time.Instant

data class ChatMember(
    val roomId: Long,
    val userId: Long,
    val type: ChatMemberType,
    val lastReadSeq: Long,
    val joinedAt: Instant,
) {
    companion object {
        const val MAX_NEARBY_ROOMS = 3
    }
}
