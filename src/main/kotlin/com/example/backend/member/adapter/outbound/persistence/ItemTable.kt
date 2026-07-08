package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object ItemTable : Table("items") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 100)
    val description = text("description").nullable()
    override val primaryKey = PrimaryKey(id)
}
