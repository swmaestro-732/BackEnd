package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object UserItemTable : Table("user_items") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val itemId = long("item_id")
    val itemCnt = integer("item_cnt")
    val acquiredAt = timestamp("acquired_at")
    val expiredAt = timestamp("expired_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
