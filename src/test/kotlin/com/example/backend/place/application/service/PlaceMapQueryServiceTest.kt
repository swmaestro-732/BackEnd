package com.example.backend.place.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.place.application.port.inbound.dto.PlaceMapSort
import com.example.backend.place.application.port.outbound.PlaceMapBucket
import com.example.backend.place.application.port.outbound.PlaceMapHits
import com.example.backend.place.application.port.outbound.PlaceMapSearchPort
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class PlaceMapQueryServiceTest {
    private val viewport = Viewport(Coordinate(37.0, 126.0), Coordinate(38.0, 128.0))
    private val areas = mock(AreaQueryUseCase::class.java)
    private val engineCriteria = mutableListOf<PlaceSearchCriteria>()
    private val engineSorts = mutableListOf<PlaceMapSort>()
    private val hydratedIds = mutableListOf<Long>()
    private var engineHits: (PlaceSearchCriteria) -> PlaceMapHits = { PlaceMapHits(0, emptyList(), emptyList()) }
    private val engine =
        mock(PlaceMapSearchPort::class.java) { invocation ->
            val criteria = invocation.getArgument<PlaceSearchCriteria>(0)
            engineCriteria += criteria
            engineSorts += invocation.getArgument<PlaceMapSort>(2)
            engineHits(criteria)
        }
    private val places =
        mock(PlaceQueryPort::class.java) { invocation ->
            val ids = invocation.getArgument<List<Long>>(0)
            hydratedIds += ids
            ids.sorted().map(::place) // DB 는 id 순으로 돌려준다
        }
    private val service = PlaceMapQueryService(engine, places, PlaceSearchQueryPlanner(areas))

    @Test
    fun `키워드가 없어도 화면 안 전체 장소를 엔진 정렬 순서대로 조회한다`() {
        engineHits = { PlaceMapHits(3, listOf(3, 1, 2), emptyList()) }

        val result = service.searchMap("", viewport, null)

        assertEquals(3L, result.totalCount)
        assertEquals(listOf(3L, 1L, 2L), result.places.map { it.id })
        assertTrue(result.clusters.isEmpty())
        assertEquals(viewport, engineCriteria.single().viewport)
        assertTrue(engineCriteria.single().textTokens.isEmpty())
        assertEquals(PlaceMapSort.RELEVANCE, engineSorts.single())
        assertNull(engineCriteria.single().anchor)
    }

    @Test
    fun `거리순은 사용자 위치를 기준점으로 넘기고 없으면 뷰포트 중심을 쓴다`() {
        engineHits = { PlaceMapHits(1, listOf(1), emptyList()) }
        val user = Coordinate(37.9, 127.9)

        service.searchMap("", viewport, null, PlaceMapSort.DISTANCE, user)
        service.searchMap("", viewport, null, PlaceMapSort.DISTANCE, null)

        assertEquals(listOf(PlaceMapSort.DISTANCE, PlaceMapSort.DISTANCE), engineSorts)
        assertEquals(user, engineCriteria.first().anchor)
        assertEquals(Coordinate(37.5, 127.0), engineCriteria.last().anchor)
    }

    @Test
    fun `100건 초과는 단일 셀 마커를 엔진이 준 버킷 순서대로 낸다`() {
        engineHits = {
            PlaceMapHits(
                101,
                emptyList(),
                listOf(
                    PlaceMapBucket("b", Coordinate(37.9, 127.9), 1, 7),
                    PlaceMapBucket("a", Coordinate(37.51, 127.01), 1, 3),
                    PlaceMapBucket("many", Coordinate(37.2, 127.2), 99, null),
                ),
            )
        }

        val result = service.searchMap("", viewport, null, PlaceMapSort.DISTANCE, Coordinate(37.5, 127.0))

        assertEquals(listOf(7L, 3L), result.places.map { it.id })
        assertEquals(99L, result.clusters.single().count)
    }

    @Test
    fun `100건은 목록 기본 개수와 관계없이 모두 개별 마커로 반환한다`() {
        engineHits = { PlaceMapHits(100, (1L..100L).toList(), emptyList()) }

        val result = service.searchMap("", viewport, null)

        assertEquals(100, result.places.size)
        assertTrue(result.clusters.isEmpty())
    }

    @Test
    fun `100건 초과는 단일 장소 셀과 클러스터로 빠짐없이 표현한다`() {
        engineHits = {
            PlaceMapHits(
                101,
                emptyList(),
                listOf(
                    PlaceMapBucket("one", Coordinate(37.1, 127.1), 1, 7),
                    PlaceMapBucket("many", Coordinate(37.2, 127.2), 100, null),
                ),
            )
        }

        val result = service.searchMap("", viewport, null)

        assertEquals(listOf(7L), result.places.map { it.id })
        assertEquals(listOf(7L), hydratedIds)
        assertEquals(100L, result.clusters.single().count)
        assertEquals(37.2, result.clusters.single().latitude)
        assertEquals(127.2, result.clusters.single().longitude)
        assertEquals(result.totalCount, result.places.size + result.clusters.sumOf { it.count })
    }

    @Test
    fun `명시한 카테고리가 검색어에서 추론한 카테고리보다 우선한다`() {
        engineHits = { PlaceMapHits(1, listOf(1), emptyList()) }

        service.searchMap("카페", viewport, "RESTAURANT")

        assertEquals(listOf(PlaceCategory.RESTAURANT), engineCriteria.single().categories)
    }

    @Test
    fun `0건 재검색은 추론만 풀고 명시 카테고리와 뷰포트를 유지한다`() {
        `when`(areas.resolveSearchPrefixes("서울")).thenReturn(listOf("11"))
        `when`(areas.resolveSearchPrefixes("강남구")).thenReturn(listOf("11680"))
        engineHits = { criteria ->
            if (criteria.areaCodePrefixGroups.isEmpty()) {
                PlaceMapHits(1, listOf(1), emptyList())
            } else {
                PlaceMapHits(0, emptyList(), emptyList())
            }
        }

        val result = service.searchMap("서울 강남구 카페", viewport, "RESTAURANT")

        assertEquals(1L, result.totalCount)
        assertEquals(2, engineCriteria.size)
        assertEquals(listOf(listOf("11"), listOf("11680")), engineCriteria.first().areaCodePrefixGroups)
        assertTrue(engineCriteria.last().areaCodePrefixGroups.isEmpty())
        assertEquals(listOf("서울", "강남구", "카페"), engineCriteria.last().textTokens)
        assertTrue(engineCriteria.all { it.viewport == viewport && it.categories == listOf(PlaceCategory.RESTAURANT) })
    }

    @Test
    fun `검색엔진 장애는 DB 폴백 없이 503 으로 전파한다`() {
        engineHits = { throw BusinessException(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE) }

        val exception = assertThrows<BusinessException> { service.searchMap("", viewport, "CAFE") }

        assertEquals(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE, exception.errorCode)
        assertTrue(hydratedIds.isEmpty())
    }

    private fun place(id: Long): Place =
        Place.reconstitute(
            id = id,
            status = PlaceStatus.ACTIVE,
            name = "카페 $id",
            description = null,
            category = PlaceCategory.CAFE,
            location = Coordinate(37.5, 127.0),
            address = "서울",
            imageUrl = null,
            businessStatus = PlaceBusinessStatus.UNKNOWN,
            kakaoPlaceId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
        )
}
