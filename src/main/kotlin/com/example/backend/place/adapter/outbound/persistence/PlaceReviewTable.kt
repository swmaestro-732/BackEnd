package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.domain.model.PlaceReviewStatus
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다. 접근은 DSL 로만 한다(DAO 엔티티 없음).
// created_at·updated_at 은 DB 전용 DEFAULT 를 batch insert 가 못 쓰므로 클라이언트 기본값을 둔다
// (삽입 시에는 반환 객체 조립을 위해 리포지토리가 값을 명시한다).
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
