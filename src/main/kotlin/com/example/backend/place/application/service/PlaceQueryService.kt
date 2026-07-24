package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceQueryService(
    private val placeQueryPort: PlaceQueryPort,
) : PlaceQueryUseCase {
    override fun findCategoryNames(placeIds: List<Long>): Map<Long, String> =
        if (placeIds.isEmpty()) emptyMap() else placeQueryPort.findCategoryNames(placeIds)
}
