package com.example.backend.course.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object TagTable : Table("tags") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 50)
    override val primaryKey = PrimaryKey(id)
}
