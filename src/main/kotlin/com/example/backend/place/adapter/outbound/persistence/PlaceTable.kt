package com.example.backend.place.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object PlaceTable : Table("places") {
    val id = long("id").autoIncrement()
    val status = short("status")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val category = varchar("category", 50)

    // TODO: PostGIS geometry(Point,4326) — Exposed 컬럼 매핑 보류
    val address = varchar("address", 255)
    val imageUrl = text("image_url").nullable()
    val businessStatus = short("business_status")
    override val primaryKey = PrimaryKey(id)
}
