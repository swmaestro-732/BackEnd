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
 * 장소 검색은 OpenSearch 로만 한다([PlaceSearchQueryPort] — 지역·카테고리 필터 + 텍스트 검색).
 * 엔진 미가용·실패는 포트가 `PLACE_SEARCH_UNAVAILABLE`(503)로 올리고 DB 폴백은 없다 — 클라이언트가 같은 커서로 재시도한다.
 * 결과 id 는 DB 로 hydration 하며(색인엔 있지만 삭제된 장소는 자연 탈락), 커서는 엔진 오프셋 + 발급 당시 기준 장소를 담는다.
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

        val decoded = PlaceSearchCursorCodec.decode(cursor)
        if (decoded != null) {
            if (decoded.anchorPlaceId != anchorPlaceId) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "검색 기준 장소가 변경되었습니다. 처음부터 검색해 주세요.")
            }
            require(decoded.offset < MAX_RESULT_WINDOW) { "검색 범위를 좁혀서 다시 검색해 주세요." }
        }
        return search(
            query = query,
            offset = decoded?.offset ?: 0,
            textFallback = decoded?.textFallback ?: false,
            size = size,
            anchor = anchor,
            anchorPlaceId = anchorPlaceId,
        )
    }

    private fun search(
        query: String,
        offset: Int,
        textFallback: Boolean,
        size: Int,
        anchor: Coordinate?,
        anchorPlaceId: Long?,
    ): PlaceSummaryPage {
        val pageSize = minOf(size, MAX_RESULT_WINDOW - offset)
        var usedFallback = textFallback
        val criteria = buildCriteria(query, offset, usedFallback, pageSize, anchor)
        var hits = placeSearchQueryPort.search(criteria)

        // 0건 텍스트 재검색은 사전 필터만 풀고 기준 장소는 유지한다
        val hadFilters = criteria.categories.isNotEmpty() || criteria.areaCodePrefixes.isNotEmpty()
        if (offset == 0 && !usedFallback && hits.totalCount == 0L && hadFilters) {
            usedFallback = true
            hits = placeSearchQueryPort.search(buildCriteria(query, offset, usedFallback, pageSize, anchor))
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
                    PlaceSearchCursorCodec.encode(offset + pageSize, usedFallback, anchorPlaceId)
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
