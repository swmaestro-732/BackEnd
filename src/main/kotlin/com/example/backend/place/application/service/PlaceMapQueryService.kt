package com.example.backend.place.application.service

import com.example.backend.common.geo.Viewport
import com.example.backend.place.application.port.inbound.PlaceMapQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceMapCluster
import com.example.backend.place.application.port.inbound.dto.PlaceMapResult
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.outbound.PlaceMapSearchPort
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.domain.model.PlaceCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 지도 검색 — 뷰포트 안 장소를 개별 마커(100건 이하) 또는 격자 클러스터로 내려준다. 엔진 미가용·실패는 503(DB 폴백 없음). */
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
    ): PlaceMapResult {
        require(
            viewport.southWest.latitude < viewport.northEast.latitude &&
                viewport.southWest.longitude < viewport.northEast.longitude,
        ) { "지도 영역의 너비와 높이는 0보다 커야 합니다." }

        val plan = queryPlanner.plan(query)
        val explicitCategory = category?.let { PlaceCategory.valueOf(it) }
        val criteria =
            PlaceSearchCriteria(
                textTokens = plan.textTokens,
                categories = explicitCategory?.let { listOf(it) } ?: plan.categories,
                areaCodePrefixes = plan.areaCodePrefixes,
                viewport = viewport,
                from = 0,
                size = PlaceMapGrid.MAX_MARKERS,
            )
        val precision = PlaceMapGrid.precision(viewport)

        var hits = searchPort.searchMap(criteria, precision)
        if (hits.totalCount == 0L && (plan.categories.isNotEmpty() || plan.areaCodePrefixes.isNotEmpty())) {
            hits =
                searchPort.searchMap(
                    criteria.copy(
                        textTokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() },
                        categories = explicitCategory?.let { listOf(it) }.orEmpty(),
                        areaCodePrefixes = emptyList(),
                    ),
                    precision,
                )
        }
        val ids =
            if (hits.totalCount <=
                PlaceMapGrid.MAX_MARKERS
            ) {
                hits.ids
            } else {
                hits.buckets.mapNotNull { it.singlePlaceId }
            }
        val places =
            placeQueryPort.findPlacesById(ids).map {
                PlaceSummary(
                    it.id!!,
                    it.name,
                    it.category.name,
                    it.imageUrl,
                    it.location.latitude,
                    it.location.longitude,
                    it.address,
                    it.areaCode,
                )
            }
        val clusters =
            if (hits.totalCount <=
                PlaceMapGrid.MAX_MARKERS
            ) {
                emptyList()
            } else {
                hits.buckets.filter { it.count > 1 }.map {
                    PlaceMapCluster(it.key, it.center.latitude, it.center.longitude, it.count)
                }
            }
        return PlaceMapResult(places.size.toLong() + clusters.sumOf { it.count }, places, clusters)
    }
}
