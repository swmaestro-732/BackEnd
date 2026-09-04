package com.example.backend.place.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(PlaceReviewTagEntity)·DSL 공용.
internal object PlaceReviewTagTable : LongIdTable("place_review_tags") {
    val label = varchar("label", 20)
    val icon = varchar("icon", 20)
}

/** place_review_tags 테이블의 DAO 엔티티([PlaceReviewTagTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class PlaceReviewTagEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<PlaceReviewTagEntity>(PlaceReviewTagTable)

    var label by PlaceReviewTagTable.label
    var icon by PlaceReviewTagTable.icon
}
