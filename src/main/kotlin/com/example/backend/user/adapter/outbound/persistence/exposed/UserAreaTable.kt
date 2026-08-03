package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object UserAreaTable : Table("user_areas") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val area = varchar("area", 20)
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
