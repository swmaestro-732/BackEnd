package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object DeviceTable : Table("devices") {
    val deviceId = varchar("device_id", 128)
    val userId = long("user_id").nullable()
    val fcmToken = varchar("fcm_token", 1024).nullable()
    val platform = varchar("platform", 16).nullable()
    val appBuild = integer("app_build").nullable()
    val marketingAgreedAt = timestamp("marketing_agreed_at").nullable()
    val nightAgreedAt = timestamp("night_agreed_at").nullable()
    val createdAt = timestamp("created_at").databaseGenerated()
    val lastSeenAt = timestamp("last_seen_at").databaseGenerated()
    override val primaryKey = PrimaryKey(deviceId)
}
