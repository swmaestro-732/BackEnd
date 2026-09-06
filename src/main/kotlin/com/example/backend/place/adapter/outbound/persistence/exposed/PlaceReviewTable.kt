package com.example.backend.place.adapter.outbound.persistence.exposed

import com.example.backend.place.domain.model.PlaceReviewStatus
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(PlaceReviewEntity)·DSL 공용.
// created_at·updated_at 은 DAO(.new) batch insert 가 DB 전용 DEFAULT 를 못 쓰므로 클라이언트 기본값을 둔다.
internal object PlaceReviewTable : LongIdTable("place_reviews") {
    val placeId = long("place_id") // 같은 도메인(place) 참조 — 스키마에 FK 있음
    val status = enumerationByName<PlaceReviewStatus>("status", 32)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
    val deletedAt = timestamp("deleted_at").nullable()
    val userId = long("user_id") // cross-domain(user): FK 없음
    val rating = short("rating")
    val content = text("content").nullable()
}

/** place_reviews 테이블의 DAO 엔티티([PlaceReviewTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class PlaceReviewEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<PlaceReviewEntity>(PlaceReviewTable)

    var placeId by PlaceReviewTable.placeId
    var status by PlaceReviewTable.status
    var createdAt by PlaceReviewTable.createdAt
    var updatedAt by PlaceReviewTable.updatedAt
    var deletedAt by PlaceReviewTable.deletedAt
    var userId by PlaceReviewTable.userId
    var rating by PlaceReviewTable.rating
    var content by PlaceReviewTable.content
}
