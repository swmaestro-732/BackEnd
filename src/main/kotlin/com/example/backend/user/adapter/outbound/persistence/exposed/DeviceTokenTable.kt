package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object DeviceTokenTable : Table("device_tokens") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val deviceId = varchar("device_id", 128)
    val fcmToken = varchar("fcm_token", 1024)
    val platform = varchar("platform", 16).nullable()
    val appBuild = integer("app_build").nullable()
    val createdAt = timestamp("created_at").databaseGenerated()
    val lastSeenAt = timestamp("last_seen_at").databaseGenerated()
    override val primaryKey = PrimaryKey(id)
}
