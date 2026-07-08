package com.example.backend.place.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object PlaceReviewTable : Table("place_reviews") {
    val id = long("id").autoIncrement()
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val status = short("status")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val userId = long("user_id") // cross-domain(user): FK 없음
    val rating = short("rating")
    val content = text("content").nullable()
    override val primaryKey = PrimaryKey(id)
}
