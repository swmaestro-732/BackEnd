package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.common.persistence.postgis.geographyPoint
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object PlaceTable : Table("places") {
    val id = long("id").autoIncrement()
    val status = enumerationByName<PlaceStatus>("status", 32)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val category = enumerationByName<PlaceCategory>("category", 50)
    val location = geographyPoint("location")
    val address = varchar("address", 255)
    val imageUrl = text("image_url").nullable()
    val businessStatus = enumerationByName<PlaceBusinessStatus>("business_status", 32)
    override val primaryKey = PrimaryKey(id)
}
