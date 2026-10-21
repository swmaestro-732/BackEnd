package com.example.backend.chat.adapter.outbound.persistence.exposed

import com.example.backend.chat.domain.model.ChatRegionType
import org.jetbrains.exposed.v1.core.Table

internal object ChatRoomRegionTable : Table("chat_room_regions") {
    val roomId = long("room_id")
    val regionType = enumerationByName<ChatRegionType>("region_type", 32)
    val regionValue = varchar("region_value", 20)

    override val primaryKey = PrimaryKey(regionType, regionValue)
}
