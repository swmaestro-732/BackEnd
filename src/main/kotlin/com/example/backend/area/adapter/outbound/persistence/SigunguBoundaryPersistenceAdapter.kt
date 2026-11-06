package com.example.backend.area.adapter.outbound.persistence

import com.example.backend.area.adapter.outbound.persistence.exposed.repository.SigunguBoundaryRepository
import com.example.backend.area.application.port.outbound.SigunguBoundaryPort
import com.example.backend.common.geo.Coordinate
import org.springframework.stereotype.Component

@Component
class SigunguBoundaryPersistenceAdapter(
    private val sigunguBoundaryRepository: SigunguBoundaryRepository,
) : SigunguBoundaryPort {
    private val adjacency: Map<String, List<String>> by lazy {
        sigunguBoundaryRepository.findAllAdjacency().groupBy({ it.first }, { it.second })
    }

    override fun findCodeContaining(coordinate: Coordinate): String? =
        sigunguBoundaryRepository.findCodeContaining(coordinate)

    override fun findNearestCode(
        coordinate: Coordinate,
        maxMeters: Double,
    ): String? = sigunguBoundaryRepository.findNearestCode(coordinate, maxMeters)

    override fun findAdjacentCodes(sigunguCode: String): List<String> = adjacency[sigunguCode].orEmpty()
}
