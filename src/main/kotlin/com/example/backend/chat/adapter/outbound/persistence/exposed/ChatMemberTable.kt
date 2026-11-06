package com.example.backend.chat.adapter.outbound.persistence.exposed

import com.example.backend.chat.domain.model.ChatMemberType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

internal object ChatMemberTable : Table("chat_members") {
    val roomId = long("room_id")
    val userId = long("user_id")
    val type = enumerationByName<ChatMemberType>("type", 32)
    val lastReadSeq = long("last_read_seq").default(0L)
    val joinedAt = timestamp("joined_at").clientDefault { Clock.System.now() }

    override val primaryKey = PrimaryKey(roomId, userId)
}
