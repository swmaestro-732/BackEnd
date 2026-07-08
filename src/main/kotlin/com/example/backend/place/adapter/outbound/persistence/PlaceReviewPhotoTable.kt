package com.example.backend.place.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object PlaceReviewPhotoTable : Table("place_review_photos") {
    val id = long("id").autoIncrement()
    val placeReviewId = long("place_review_id")
    val imageUrl = text("image_url").nullable()
    val orderNo = short("order_no")
    override val primaryKey = PrimaryKey(id)
}
