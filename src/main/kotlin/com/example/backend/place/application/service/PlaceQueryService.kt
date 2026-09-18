package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.inbound.dto.PlaceSummaryPage
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.application.port.outbound.PlaceSearchQueryPort
import com.example.backend.place.domain.model.Place
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장소 검색은 OpenSearch 우선([PlaceSearchQueryPort] — 지역·카테고리 필터 + 텍스트 검색), 미가용·실패 시
 * DB LIKE([PlaceQueryPort.searchByName]) 폴백이다. 커서는 발급 경로를 자기 기술하므로([PlaceSearchCursorCodec])
 * 한 번 시작한 페이지네이션은 같은 경로에서 이어간다(정렬 방식이 달라 중간 전환하면 중복·누락이 생긴다).
 *
 * OpenSearch 호출이 읽기 트랜잭션 안에서 돌지만, 클라이언트 타임아웃이 2초로 캡핑돼 있어
 * (OpenSearchConfig) 커넥션 점유가 짧다 — 별도 트랜잭션 분리는 하지 않는다.
 */
@Service
@Transactional(readOnly = true)
class PlaceQueryService(
    private val placeQueryPort: PlaceQueryPort,
    private val placeSearchQueryPort: PlaceSearchQueryPort,
    private val queryPlanner: PlaceSearchQueryPlanner,
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
        anchorPlaceId: Long?,
    ): PlaceSummaryPage {
        require(size in 1..50) { "조회 개수는 1~50 범위여야 합니다." }
        require(anchorPlaceId == null || anchorPlaceId > 0) { "기준 장소 ID는 양수여야 합니다." }
        val anchor =
            anchorPlaceId?.let { id ->
                placeQueryPort.findPlacesById(listOf(id)).firstOrNull()?.location
                    ?: throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND)
            }
        if (query.isBlank()) return PlaceSummaryPage(items = emptyList(), totalCount = 0, hasNext = false)
        return when (val decoded = PlaceSearchCursorCodec.decode(cursor)) {
            is PlaceSearchCursor.DbKeyset -> {
                validateAnchor(anchor == null)
                searchFromDb(query, decoded.lastId, size)
            }

            is PlaceSearchCursor.DbNearby -> {
                validateAnchor(decoded.anchorPlaceId == anchorPlaceId && anchor != null)
                searchNearbyFromDb(query, anchor!!, anchorPlaceId, decoded.offset, size)
            }

            is PlaceSearchCursor.Offset -> {
                validateAnchor(decoded.anchorPlaceId == anchorPlaceId)
                require(decoded.offset < MAX_RESULT_WINDOW) { "검색 범위를 좁혀서 다시 검색해 주세요." }
                searchFromEngine(query, decoded.offset, decoded.textFallback, size, anchor, anchorPlaceId)
                    ?: throw BusinessException(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE)
            }

            null -> {
                searchFromEngine(query, 0, false, size, anchor, anchorPlaceId)
                    ?: if (anchor !=
                        null
                    ) {
                        searchNearbyFromDb(query, anchor, anchorPlaceId, 0, size)
                    } else {
                        searchFromDb(query, null, size)
                    }
            }
        }
    }

    private fun searchFromEngine(
        query: String,
        offset: Int,
        textFallback: Boolean,
        size: Int,
        anchor: Coordinate?,
        anchorPlaceId: Long?,
    ): PlaceSummaryPage? {
        val pageSize = minOf(size, MAX_RESULT_WINDOW - offset)
        var usedFallback = textFallback
        val criteria = buildCriteria(query, offset, usedFallback, pageSize, anchor)
        var hits = placeSearchQueryPort.search(criteria) ?: return null

        // 0건 텍스트 재검색은 사전 필터만 풀고 기준 장소는 유지한다
        val hadFilters = criteria.categories.isNotEmpty() || criteria.areaCodePrefixes.isNotEmpty()
        if (offset == 0 && !usedFallback && hits.totalCount == 0L && hadFilters) {
            usedFallback = true
            hits =
                placeSearchQueryPort.search(buildCriteria(query, offset, usedFallback, pageSize, anchor)) ?: return null
        }

        // hydration — 색인엔 있지만 DB 에서 삭제된 id 는 자연 탈락
        val byId = placeQueryPort.findPlacesById(hits.ids).associateBy { it.id }
        val items = hits.ids.mapNotNull { byId[it] }.map { it.toSummary() }

        val hasNext = offset + pageSize < minOf(hits.totalCount, MAX_RESULT_WINDOW.toLong())
        return PlaceSummaryPage(
            items = items,
            totalCount = hits.totalCount.toInt(),
            hasNext = hasNext,
            nextCursor =
                if (hasNext) {
                    PlaceSearchCursorCodec.encodeOffset(
                        offset + pageSize,
                        usedFallback,
                        anchorPlaceId,
                    )
                } else {
                    null
                },
        )
    }

    private fun buildCriteria(
        query: String,
        offset: Int,
        textFallback: Boolean,
        size: Int,
        anchor: Coordinate?,
    ): PlaceSearchCriteria {
        if (textFallback) {
            // 0건 폴백 — 사전을 거치지 않고 전 토큰을 텍스트로 검색한다.
            return PlaceSearchCriteria(
                textTokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() },
                categories = emptyList(),
                areaCodePrefixes = emptyList(),
                viewport = null,
                from = offset,
                size = size,
                anchor = anchor,
                originalQuery = query.trim(),
            )
        }
        val plan = queryPlanner.plan(query)
        return PlaceSearchCriteria(
            textTokens = plan.textTokens,
            categories = plan.categories,
            areaCodePrefixes = plan.areaCodePrefixes,
            viewport = null,
            from = offset,
            size = size,
            anchor = anchor,
            originalQuery = query.trim(),
        )
    }

    /** DB LIKE 폴백 경로 — hasNext 판별을 위해 한 건 더 읽고, 초과분은 잘라낸다. */
    private fun searchFromDb(
        query: String,
        afterId: Long?,
        size: Int,
    ): PlaceSummaryPage {
        val rows = placeQueryPort.searchByName(query, afterId?.toString(), size + 1)
        val hasNext = rows.size > size
        val items = rows.take(size).map { it.toSummary() }
        return PlaceSummaryPage(
            items = items,
            totalCount = placeQueryPort.countByName(query).toInt(),
            hasNext = hasNext,
            nextCursor = if (hasNext) PlaceSearchCursorCodec.encodeDbKeyset(items.last().id) else null,
        )
    }

    private fun searchNearbyFromDb(
        query: String,
        anchor: Coordinate,
        anchorPlaceId: Long,
        offset: Int,
        size: Int,
    ): PlaceSummaryPage {
        require(offset < MAX_RESULT_WINDOW) { "검색 범위를 좁혀서 다시 검색해 주세요." }
        val pageSize = minOf(size, MAX_RESULT_WINDOW - offset)
        val rows = placeQueryPort.searchNearbyByName(query, anchor, offset, pageSize + 1)
        val hasNext = rows.size > pageSize && offset + pageSize < MAX_RESULT_WINDOW
        return PlaceSummaryPage(
            items = rows.take(pageSize).map { it.toSummary() },
            totalCount = placeQueryPort.countByName(query).toInt(),
            hasNext = hasNext,
            nextCursor = if (hasNext) PlaceSearchCursorCodec.encodeDbNearby(offset + pageSize, anchorPlaceId) else null,
        )
    }

    private fun validateAnchor(valid: Boolean) {
        if (!valid) throw BusinessException(CommonErrorCode.INVALID_INPUT, "검색 기준 장소가 변경되었습니다. 처음부터 검색해 주세요.")
    }

    private companion object {
        const val MAX_RESULT_WINDOW = 10_000
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
