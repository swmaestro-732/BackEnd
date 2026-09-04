package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceRepository
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.domain.model.Place
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [PlaceQueryPort] 를 구현한다.
 * 실제 테이블 접근은 [PlaceRepository] 에 위임하고, 이 어댑터는 유스케이스 계약만 맞춘다.
 */
@Component
class PlaceQueryAdapter(
    private val placeRepository: PlaceRepository,
) : PlaceQueryPort {
    override fun findPlacesById(placeIds: List<Long>): List<Place> {
        if (placeIds.isEmpty()) return emptyList()
        return placeRepository.findByIds(placeIds).map { it.toDomain() }
    }

    override fun searchByName(
        query: String,
        cursor: String?,
        limit: Int,
    ): List<Place> {
        if (query.isBlank()) return emptyList()
        return placeRepository.searchByName(query.trim(), cursor?.toLong(), limit).map { it.toDomain() }
    }

    override fun countByName(query: String): Long {
        if (query.isBlank()) return 0
        return placeRepository.countByName(query.trim())
    }
}
