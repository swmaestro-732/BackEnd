package com.example.backend.area.adapter.outbound.persistence.exposed.repository

import com.example.backend.area.adapter.outbound.persistence.exposed.SigunguAdjacencyTable
import com.example.backend.area.adapter.outbound.persistence.exposed.SigunguBoundaryTable
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.persistence.postgis.geometryPoint
import com.example.backend.common.persistence.postgis.stContains
import com.example.backend.common.persistence.postgis.stDWithinDegrees
import com.example.backend.common.persistence.postgis.stDistanceMeters
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class SigunguBoundaryRepository {
    fun findCodeContaining(coordinate: Coordinate): String? =
        SigunguBoundaryTable
            .select(SigunguBoundaryTable.sigunguCode)
            .where { SigunguBoundaryTable.geom.stContains(coordinate.toGeometry()) }
            .limit(1)
            .firstOrNull()
            ?.get(SigunguBoundaryTable.sigunguCode)

    fun findNearestCode(
        coordinate: Coordinate,
        maxMeters: Double,
    ): String? {
        val point = coordinate.toGeometry()
        val distance = SigunguBoundaryTable.geom.stDistanceMeters(point)
        return SigunguBoundaryTable
            .select(SigunguBoundaryTable.sigunguCode, distance)
            // 미터 거리 계산은 인덱스를 못 타서, 도 단위 필터로 후보를 먼저 줄인다(285ms → 2ms).
            .where { SigunguBoundaryTable.geom.stDWithinDegrees(point, maxMeters / MIN_METERS_PER_DEGREE) }
            .orderBy(distance)
            .limit(1)
            .firstOrNull()
            ?.takeIf { it[distance] <= maxMeters }
            ?.get(SigunguBoundaryTable.sigunguCode)
    }

    fun findAllAdjacency(): List<Pair<String, String>> =
        SigunguAdjacencyTable
            .selectAll()
            .map { it[SigunguAdjacencyTable.sigunguCode] to it[SigunguAdjacencyTable.neighborCode] }

    private fun Coordinate.toGeometry() = geometryPoint(latitude, longitude)

    private companion object {
        // 경도 1도는 위도가 높을수록 짧아진다(국내 약 87~93km). 후보를 놓치지 않게 작은 값으로 나눈다.
        const val MIN_METERS_PER_DEGREE = 80_000.0
    }
}
