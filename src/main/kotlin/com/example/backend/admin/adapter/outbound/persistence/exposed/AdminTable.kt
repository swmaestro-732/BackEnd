package com.example.backend.admin.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object AdminTable : Table("admins") {
    val email = varchar("email", 320)
    val googleSub = varchar("google_sub", 255).nullable()
    val createdAt = timestamp("created_at").databaseGenerated()
    override val primaryKey = PrimaryKey(email)
}
