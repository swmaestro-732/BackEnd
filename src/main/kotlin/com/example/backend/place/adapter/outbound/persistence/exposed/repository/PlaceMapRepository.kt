package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.persistence.postgis.geoHash
import com.example.backend.common.persistence.postgis.intersectsViewport
import com.example.backend.common.persistence.postgis.stX
import com.example.backend.common.persistence.postgis.stY
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.application.port.outbound.PlaceMapBucket
import com.example.backend.place.application.port.outbound.PlaceMapHits
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.domain.model.PlaceStatus
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.avg
import org.jetbrains.exposed.v1.core.compoundOr
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/** DB 폴백도 LIMIT으로 장소를 버리지 않고 전체 결과를 격자별로 집계한다. */
@Repository
class PlaceMapRepository {
    fun search(
        criteria: PlaceSearchCriteria,
        precision: Int,
    ): PlaceMapHits {
        val condition = condition(criteria)
        val hash = PlaceTable.location.geoHash(precision)
        val count = PlaceTable.id.count()
        val minId = PlaceTable.id.min()
        val latitude = PlaceTable.location.stY().avg(scale = 10)
        val longitude = PlaceTable.location.stX().avg(scale = 10)
        val buckets =
            PlaceTable
                .select(hash, count, minId, latitude, longitude)
                .where { condition }
                .groupBy(hash)
                .map { row ->
                    PlaceMapBucket(
                        key = row[hash],
                        center = Coordinate(row[latitude]!!.toDouble(), row[longitude]!!.toDouble()),
                        count = row[count],
                        singlePlaceId = if (row[count] == 1L) row[minId]!!.value else null,
                    )
                }.sortedBy { it.key }
        val total = buckets.sumOf { it.count }
        val ids =
            if (total <= criteria.size) {
                PlaceTable
                    .select(PlaceTable.id)
                    .where {
                        condition
                    }.orderBy(PlaceTable.id)
                    .limit(criteria.size)
                    .map { it[PlaceTable.id].value }
            } else {
                emptyList()
            }
        return PlaceMapHits(total, ids, buckets)
    }

    private fun condition(criteria: PlaceSearchCriteria): Op<Boolean> {
        var condition: Op<Boolean> =
            PlaceTable.deletedAt.isNull() and (PlaceTable.status eq PlaceStatus.ACTIVE) and
                PlaceTable.location.intersectsViewport(requireNotNull(criteria.viewport))
        if (criteria.categories.isNotEmpty()) condition = condition and (PlaceTable.category inList criteria.categories)
        if (criteria.areaCodePrefixes.isNotEmpty()) {
            condition =
                condition and
                criteria.areaCodePrefixes.map { PlaceTable.areaCode like "${it.escapeLike()}%" }.compoundOr()
        }
        criteria.textTokens.forEach { token ->
            val pattern = "%${token.lowercase().escapeLike()}%"
            condition = condition and (
                (PlaceTable.name.lowerCase() like pattern) or
                    (PlaceTable.address.lowerCase() like pattern) or (PlaceTable.description.lowerCase() like pattern)
            )
        }
        return condition
    }

    private fun String.escapeLike() = replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
