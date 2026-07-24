package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.application.port.outbound.PlaceQueryPort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/**
 * 아웃바운드 어댑터 — [PlaceQueryPort] 를 Exposed 로 구현한다.
 * 삭제되지 않은 장소만 대상으로 placeId → 카테고리 이름을 반환한다.
 */
@Repository
class PlaceQueryAdapter : PlaceQueryPort {
    override fun findCategoryNames(placeIds: List<Long>): Map<Long, String> {
        if (placeIds.isEmpty()) return emptyMap()
        return PlaceTable
            .select(PlaceTable.id, PlaceTable.category)
            .where { (PlaceTable.id inList placeIds) and PlaceTable.deletedAt.isNull() }
            .associate { it[PlaceTable.id] to it[PlaceTable.category].name }
    }
}
