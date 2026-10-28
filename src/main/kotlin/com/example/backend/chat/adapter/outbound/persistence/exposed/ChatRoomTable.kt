package com.example.backend.chat.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

internal object ChatRoomTable : LongIdTable("chat_rooms") {
    val name = varchar("name", 50)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
}
