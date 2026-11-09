package com.example.backend.admin.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

internal object AdminActionTable : LongIdTable("admin_actions") {
    val adminEmail = varchar("admin_email", 320)
    val targetType = varchar("target_type", 32)
    val targetId = long("target_id")
    val action = varchar("action", 32)
    val memo = varchar("memo", 500).nullable()
    val createdAt = timestamp("created_at").databaseGenerated()
}
