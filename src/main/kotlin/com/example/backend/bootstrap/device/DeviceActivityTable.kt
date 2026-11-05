package com.example.backend.bootstrap.device

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date

internal object DeviceActivityTable : Table("device_daily_activity") {
    val deviceId = varchar("device_id", 128)
    val activeDate = date("active_date")
    val userId = long("user_id").nullable()
    override val primaryKey = PrimaryKey(deviceId, activeDate)
}
