package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import com.example.backend.place.application.port.inbound.PlaceMapQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceMapCluster
import com.example.backend.place.application.port.inbound.dto.PlaceMapResult
import com.example.backend.place.application.port.inbound.dto.PlaceMapSort
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.outbound.PlaceMapHits
import com.example.backend.place.application.port.outbound.PlaceMapSearchPort
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 지도 검색 — 뷰포트 안 장소를 개별 마커(100건 이하) 또는 격자 클러스터로 내려준다. 엔진 미가용·실패는 503(DB 폴백 없음).
 * 정렬은 개별 마커 순서에만 적용된다 — 히트·단일 셀 버킷 모두 엔진이 정렬해 주고 여기서는 그 순서를 보존한다.
 */
@Service
@Transactional(readOnly = true)
class PlaceMapQueryService(
    private val searchPort: PlaceMapSearchPort,
    private val placeQueryPort: PlaceQueryPort,
    private val queryPlanner: PlaceSearchQueryPlanner,
) : PlaceMapQueryUseCase {
    override fun searchMap(
        query: String,
        viewport: Viewport,
        category: String?,
        sort: PlaceMapSort,
        userLocation: Coordinate?,
    ): PlaceMapResult {
        val hits = searchWithTextFallback(query, viewport, category, sort, userLocation)
        return toMapResult(hits)
    }

    private fun searchWithTextFallback(
        query: String,
        viewport: Viewport,
        category: String?,
        sort: PlaceMapSort,
        userLocation: Coordinate?,
    ): PlaceMapHits {
        val origin = if (sort == PlaceMapSort.DISTANCE) userLocation ?: viewport.center() else null
        val plan = queryPlanner.plan(query)
        val explicitCategory = category?.let { PlaceCategory.valueOf(it) }
        val criteria = buildCriteria(plan, viewport, explicitCategory, origin)
        val precision = PlaceMapGrid.precision(viewport)

        val hits = searchPort.searchMap(criteria, precision, sort)
        val hasInferredFilters = plan.categories.isNotEmpty() || plan.areaCodePrefixGroups.isNotEmpty()
        if (hits.totalCount != 0L || !hasInferredFilters) return hits

        return searchPort.searchMap(
            criteria.copy(
                textTokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() },
                categories = explicitCategory?.let { listOf(it) }.orEmpty(),
                areaCodePrefixGroups = emptyList(),
            ),
            precision,
            sort,
        )
    }

    private fun buildCriteria(
        plan: PlaceSearchPlan,
        viewport: Viewport,
        explicitCategory: PlaceCategory?,
        origin: Coordinate?,
    ): PlaceSearchCriteria =
        PlaceSearchCriteria(
            textTokens = plan.textTokens,
            categories = explicitCategory?.let { listOf(it) } ?: plan.categories,
            areaCodePrefixGroups = plan.areaCodePrefixGroups,
            viewport = viewport,
            from = 0,
            size = PlaceMapGrid.MAX_MARKERS,
            anchor = origin,
        )

    private fun toMapResult(hits: PlaceMapHits): PlaceMapResult {
        val clustered = hits.totalCount > PlaceMapGrid.MAX_MARKERS
        val ids =
            if (!clustered) {
                hits.ids
            } else {
                hits.buckets.mapNotNull { it.singlePlaceId }
            }
        val places = findPlacesInOrder(ids)
        val clusters =
            if (!clustered) {
                emptyList()
            } else {
                hits.buckets.filter { it.count > 1 }.map {
                    PlaceMapCluster(it.key, it.center.latitude, it.center.longitude, it.count)
                }
            }
        return PlaceMapResult(places.size.toLong() + clusters.sumOf { it.count }, places, clusters)
    }

    private fun findPlacesInOrder(ids: List<Long>): List<PlaceSummary> {
        // 엔진 정렬 순서를 보존한다 — DB 조회 결과는 id 순이라 그대로 쓰면 정렬이 깨진다.
        val byId = placeQueryPort.findPlacesById(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it] }.map { it.toSummary() }
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

    private fun Viewport.center() =
        Coordinate(
            (southWest.latitude + northEast.latitude) / 2,
            (southWest.longitude + northEast.longitude) / 2,
        )
}
