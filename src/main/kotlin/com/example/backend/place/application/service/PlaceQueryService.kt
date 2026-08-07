package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.inbound.dto.PlaceSummaryPage
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

    override fun searchByName(
        query: String,
        cursor: String?,
        size: Int,
    ): PlaceSummaryPage {
        if (query.isBlank()) return PlaceSummaryPage(items = emptyList(), totalCount = 0, hasNext = false)

        // hasNext 판별을 위해 한 건 더 읽고, 초과분은 잘라낸다.
        val rows = placeQueryPort.searchByName(query, cursor, size + 1)
        val hasNext = rows.size > size
        return PlaceSummaryPage(
            items = rows.take(size).map { it.toSummary() },
            totalCount = placeQueryPort.countByName(query).toInt(),
            hasNext = hasNext,
        )
    }

    private fun Place.toSummary(): PlaceSummary =
        PlaceSummary(
            id = id!!,
            name = name,
            category = category.name,
            imageUrl = imageUrl,
            latitude = location.latitude,
            longitude = location.longitude,
            address = address,
            areaCode = areaCode,
        )
}
