package com.example.backend.place.adapter.outbound.persistence.exposed

import com.example.backend.place.adapter.outbound.persistence.PlaceEntity
import com.example.backend.place.adapter.outbound.persistence.PlaceTable
import com.example.backend.place.domain.model.Place
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.springframework.stereotype.Repository

/**
 * places 테이블 접근 리포지토리 — 삭제되지 않은 장소의 조회를 담당한다.
 * 조회는 DAO([PlaceEntity])로 하고, 읽기 모델은 순수 도메인 [Place] 로 변환해 반환한다.
 */
@Repository
class PlaceRepository {
    /** deleted_at IS NULL 인 장소들을 id 목록으로 읽어 도메인으로 변환한다. */
    fun findByIds(placeIds: List<Long>): List<Place> =
        PlaceEntity
            .find { (PlaceTable.id inList placeIds) and PlaceTable.deletedAt.isNull() }
            .map { it.toDomain() }
}
