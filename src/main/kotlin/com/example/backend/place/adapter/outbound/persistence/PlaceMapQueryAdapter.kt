package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceMapRepository
import com.example.backend.place.application.port.outbound.PlaceMapHits
import com.example.backend.place.application.port.outbound.PlaceMapQueryPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import org.springframework.stereotype.Component

@Component
class PlaceMapQueryAdapter(
    private val repository: PlaceMapRepository,
) : PlaceMapQueryPort {
    override fun searchMap(
        criteria: PlaceSearchCriteria,
        precision: Int,
    ): PlaceMapHits = repository.search(criteria, precision)
}
