package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceBusinessHourRepository
import com.example.backend.place.application.port.outbound.PlaceBusinessHourPersistencePort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessHour
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [PlaceBusinessHourPersistencePort] 를 [PlaceBusinessHourRepository] 에 위임해 구현한다. */
@Component
class PlaceBusinessHourPersistenceAdapter(
    private val repository: PlaceBusinessHourRepository,
) : PlaceBusinessHourPersistencePort {
    override fun findUnsynced(placeIds: List<Long>): List<Place> =
        repository.findUnsynced(placeIds).map { it.toDomain() }

    override fun replaceBusinessHours(
        placeId: Long,
        hours: List<PlaceBusinessHour>,
    ) = repository.replaceBusinessHours(placeId, hours)
}
