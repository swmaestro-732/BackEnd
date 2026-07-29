package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.outbound.PlaceQueryPort
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
            placeQueryPort.findPlacesById(placeIds).map {
                PlaceSummary(
                    id = it.id!!,
                    name = it.name,
                    category = it.category.name,
                    latitude = it.location.latitude,
                    longitude = it.location.longitude,
                )
            }
        }
}
