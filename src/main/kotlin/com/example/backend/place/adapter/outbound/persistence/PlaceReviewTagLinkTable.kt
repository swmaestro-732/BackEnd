package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.domain.model.PlaceReviewTag
import org.jetbrains.exposed.v1.core.Table

// 태그 마스터(place_review_tags)는 V4 에서 없앴다 — 태그는 코드(enum) 정본이라 링크 테이블이 enum 이름을 직접 든다.
internal object PlaceReviewTagLinkTable : Table("place_review_tag_links") {
    val placeReviewId = long("place_review_id")
    val tag = enumerationByName<PlaceReviewTag>("tag", 32)
    override val primaryKey = PrimaryKey(placeReviewId, tag)
}
