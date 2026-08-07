package com.example.backend.user.adapter.outbound.persistence.exposed

import com.example.backend.user.domain.model.SavedPlace
import com.example.backend.user.domain.model.SavedPlaceCategory
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import kotlin.time.toJavaInstant

// LongIdTable 이 id(EntityID<Long>, "id" 컬럼)·primaryKey 를 제공한다 → DAO(SavedPlaceEntity)·DSL 공용.
// created_at 은 클라이언트 기본값을 둔다 — DAO(.new)는 batch insert 라 DB 전용 DEFAULT 를 못 태우기 때문.
internal object SavedPlaceTable : LongIdTable("saved_places") {
    val userId = long("user_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val category = enumerationByName<SavedPlaceCategory>("category", 50).nullable()
    val visited = bool("visited")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val deletedAt = timestamp("deleted_at").nullable() // 소프트 삭제 스탬프, NULL = 살아있음
}

/**
 * saved_places 테이블의 DAO 엔티티([SavedPlaceTable] 과 한 쌍이라 같은 파일에 둔다). 같은 테이블을 DSL 로도 조회할 수 있다(DAO·DSL 공용).
 * 트랜잭션에 묶인 가변 영속 객체이므로 어댑터(outbound) 밖으로 내보내지 않고, [toDomain] 으로 순수 도메인 모델로 변환해 반환한다.
 */
internal class SavedPlaceEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<SavedPlaceEntity>(SavedPlaceTable)

    var userId by SavedPlaceTable.userId
    var placeId by SavedPlaceTable.placeId
    var category by SavedPlaceTable.category
    var visited by SavedPlaceTable.visited
    var createdAt by SavedPlaceTable.createdAt
    var deletedAt by SavedPlaceTable.deletedAt

    /** DAO 엔티티를 도메인 [SavedPlace] 로 변환한다(생성된 id·created_at 은 insert 후 refresh 로 적재된 뒤라야 정확하다). */
    fun toDomain(): SavedPlace =
        SavedPlace(
            id = id.value,
            userId = userId,
            placeId = placeId,
            category = category,
            visited = visited,
            savedAt = createdAt.toJavaInstant(),
            deletedAt = deletedAt?.toJavaInstant(),
        )
}
