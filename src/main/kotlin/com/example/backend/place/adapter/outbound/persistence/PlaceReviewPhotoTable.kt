package com.example.backend.place.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다. 접근은 DSL 로만 한다(DAO 엔티티 없음).
internal object PlaceReviewPhotoTable : LongIdTable("place_review_photos") {
    val placeReviewId = long("place_review_id")
    val imageUrl = text("image_url")
    val orderNo = short("order_no")
}
