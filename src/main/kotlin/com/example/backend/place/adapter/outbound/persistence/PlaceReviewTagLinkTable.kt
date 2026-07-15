package com.example.backend.place.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object PlaceReviewTagLinkTable : Table("place_review_tag_links") {
    val placeReviewId = long("place_review_id")
    val placeReviewTagId = long("place_review_tag_id")
    override val primaryKey = PrimaryKey(placeReviewId, placeReviewTagId)
}
