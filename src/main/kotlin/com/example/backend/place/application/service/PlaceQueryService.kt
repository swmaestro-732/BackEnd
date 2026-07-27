package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.domain.model.Place
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceQueryService(
    private val placeQueryPort: PlaceQueryPort,
) : PlaceQueryUseCase {
    override fun findPlacesById(placeIds: List<Long>): List<Place> =
        if (placeIds.isEmpty()) emptyList() else placeQueryPort.findPlacesById(placeIds)
}
