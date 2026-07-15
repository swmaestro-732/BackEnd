package com.example.backend.place.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.time

internal object PlaceBusinessHourTable : Table("place_business_hours") {
    val id = long("id").autoIncrement()
    val placeId = long("place_id")
    val dayOfWeek = short("day_of_week")
    val openTime = time("open_time").nullable()
    val closeTime = time("close_time").nullable()
    override val primaryKey = PrimaryKey(id)
}
