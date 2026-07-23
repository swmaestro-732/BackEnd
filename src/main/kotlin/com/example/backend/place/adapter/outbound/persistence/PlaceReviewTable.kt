package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.domain.model.PlaceReviewStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object PlaceReviewTable : Table("place_reviews") {
    val id = long("id").autoIncrement()
    val placeId = long("place_id") // 같은 도메인(place) 참조 — 스키마에 FK 있음
    val status = enumerationByName<PlaceReviewStatus>("status", 32)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val userId = long("user_id") // cross-domain(user): FK 없음
    val rating = short("rating")
    val content = text("content").nullable()
    override val primaryKey = PrimaryKey(id)
}
