package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.domain.model.Place
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceQueryService(
    private val placeQueryPort: PlaceQueryPort,
) : PlaceQueryUseCase {
    override fun findPlacesById(placeIds: List<Long>): List<PlaceSummary> =
        if (placeIds.isEmpty()) {
            emptyList()
        } else {
            placeQueryPort.findPlacesById(placeIds).map { it.toSummary() }
        }

    override fun searchByName(query: String): List<PlaceSummary> =
        if (query.isBlank()) {
            emptyList()
        } else {
            placeQueryPort.searchByName(query).map { it.toSummary() }
        }

    private fun Place.toSummary(): PlaceSummary =
        PlaceSummary(
            id = id!!,
            name = name,
            category = category.name,
            imageUrl = imageUrl,
            latitude = location.latitude,
            longitude = location.longitude,
        )
}
