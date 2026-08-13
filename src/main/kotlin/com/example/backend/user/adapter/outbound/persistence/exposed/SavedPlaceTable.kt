package com.example.backend.user.adapter.outbound.persistence.exposed

import com.example.backend.user.domain.model.SavedPlaceCategory
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id(EntityID<Long>, "id" 컬럼)·primaryKey 를 제공한다. 접근은 전부 DSL 이다(DAO 엔티티 없음).
// created_at 은 클라이언트 기본값을 둔다 — insert 가 반환 객체 조립을 위해 시각을 직접 넘기고(SavedPlaceRepository.insert),
// 그 밖의 경로에서도 DB 전용 DEFAULT 에 기대지 않도록 하는 안전망이다.
internal object SavedPlaceTable : LongIdTable("saved_places") {
    val userId = long("user_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val category = enumerationByName<SavedPlaceCategory>("category", 50).nullable()
    val visited = bool("visited")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val deletedAt = timestamp("deleted_at").nullable() // 소프트 삭제 스탬프, NULL = 살아있음
}
