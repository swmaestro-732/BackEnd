package com.example.backend.area.application.port.outbound

import com.example.backend.common.geo.Coordinate

interface SigunguBoundaryPort {
    fun findCodeContaining(coordinate: Coordinate): String?

    fun findNearestCode(
        coordinate: Coordinate,
        maxMeters: Double,
    ): String?

    fun findAdjacentCodes(sigunguCode: String): List<String>
}
