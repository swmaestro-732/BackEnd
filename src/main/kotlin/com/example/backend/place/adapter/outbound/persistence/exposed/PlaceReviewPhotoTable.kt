package com.example.backend.place.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(PlaceReviewPhotoEntity)·DSL 공용.
internal object PlaceReviewPhotoTable : LongIdTable("place_review_photos") {
    val placeReviewId = long("place_review_id")
    val imageUrl = text("image_url")
    val orderNo = short("order_no")
}

/** place_review_photos 테이블의 DAO 엔티티([PlaceReviewPhotoTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class PlaceReviewPhotoEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<PlaceReviewPhotoEntity>(PlaceReviewPhotoTable)

    var placeReviewId by PlaceReviewPhotoTable.placeReviewId
    var imageUrl by PlaceReviewPhotoTable.imageUrl
    var orderNo by PlaceReviewPhotoTable.orderNo
}
