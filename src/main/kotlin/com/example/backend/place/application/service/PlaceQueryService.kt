package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.inbound.dto.PlaceSummaryPage
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.application.port.outbound.PlaceSearchQueryPort
import com.example.backend.place.domain.model.Place
import io.github.oshai.kotlinlogging.KotlinLogging
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
    private val log = KotlinLogging.logger {}

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

        return when (val decoded = PlaceSearchCursorCodec.decode(cursor)) {
            // db 커서는 검색엔진이 살아 있어도 DB 경로를 유지한다
            is PlaceSearchCursor.DbKeyset -> {
                searchFromDb(query, decoded.lastId, size)
            }

            is PlaceSearchCursor.Offset -> {
                searchFromEngine(query, decoded.offset, decoded.textFallback, size)
                    ?: searchFromDb(query, afterId = null, size = size).also {
                        // 오프셋 커서는 keyset 으로 번역할 수 없어 처음부터 다시 시작
                        log.warn { "장소 검색 페이지네이션 도중 검색엔진 소실 — DB 폴백으로 재시작" }
                    }
            }

            null -> {
                searchFromEngine(query, offset = 0, textFallback = false, size = size)
                    ?: searchFromDb(query, afterId = null, size = size)
            }
        }
    }

    private fun searchFromEngine(
        query: String,
        offset: Int,
        textFallback: Boolean,
        size: Int,
    ): PlaceSummaryPage? {
        var usedFallback = textFallback
        val criteria = buildCriteria(query, offset, usedFallback, size)
        var hits = placeSearchQueryPort.search(criteria) ?: return null

        val hadFilters = criteria.categories.isNotEmpty() || criteria.areaCodePrefixes.isNotEmpty()
        if (offset == 0 && !usedFallback && hits.totalCount == 0L && hadFilters) {
            usedFallback = true
            hits = placeSearchQueryPort.search(buildCriteria(query, offset, usedFallback, size)) ?: return null
        }

        // hydration — 색인엔 있지만 DB 에서 삭제된 id 는 자연 탈락
        val byId = placeQueryPort.findPlacesById(hits.ids).associateBy { it.id }
        val items = hits.ids.mapNotNull { byId[it] }.map { it.toSummary() }

        val hasNext = offset + size < hits.totalCount
        return PlaceSummaryPage(
            items = items,
            totalCount = hits.totalCount.toInt(),
            hasNext = hasNext,
            nextCursor = if (hasNext) PlaceSearchCursorCodec.encodeOffset(offset + size, usedFallback) else null,
        )
    }

    private fun buildCriteria(
        query: String,
        offset: Int,
        textFallback: Boolean,
        size: Int,
    ): PlaceSearchCriteria {
        if (textFallback) {
            // 0건 폴백 — 사전을 거치지 않고 전 토큰을 텍스트로 검색한다.
            return PlaceSearchCriteria(
                textTokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() },
                categories = emptyList(),
                areaCodePrefixes = emptyList(),
                from = offset,
                size = size,
            )
        }
        val plan = queryPlanner.plan(query)
        return PlaceSearchCriteria(
            textTokens = plan.textTokens,
            categories = plan.categories,
            areaCodePrefixes = plan.areaCodePrefixes,
            from = offset,
            size = size,
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
