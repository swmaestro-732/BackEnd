package com.example.backend.place.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.application.port.outbound.PlaceSearchHits
import com.example.backend.place.application.port.outbound.PlaceSearchQueryPort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class PlaceQueryServiceTest {
    private val anchor = place(99)
    private val criteria = mutableListOf<PlaceSearchCriteria>()
    private var anchorExists = true
    private var engineHits: () -> PlaceSearchHits = { PlaceSearchHits(listOf(1), 1) }
    private val db =
        mock(PlaceQueryPort::class.java) { invocation ->
            when (invocation.method.name) {
                "findPlacesById" -> {
                    invocation.getArgument<List<Long>>(0).mapNotNull { id ->
                        if (id == 99L) anchor.takeIf { anchorExists } else place(id)
                    }
                }

                else -> {
                    error("예상하지 못한 조회: ${invocation.method.name}")
                }
            }
        }
    private val engine =
        mock(PlaceSearchQueryPort::class.java) { invocation ->
            criteria += invocation.getArgument<PlaceSearchCriteria>(0)
            engineHits()
        }
    private val areas = mock(AreaQueryUseCase::class.java)
    private val service = PlaceQueryService(db, engine, PlaceSearchQueryPlanner(areas))

    @Test
    fun `첫 장소 검색에는 거리 기준과 뷰포트가 없다`() {
        val result = service.searchByName("블루보틀", null, 10, null)

        assertEquals(listOf(1L), result.items.map { it.id })
        assertNull(criteria.single().anchor)
        assertNull(criteria.single().viewport)
        assertEquals(listOf("블루보틀"), criteria.single().textTokens)
    }

    @Test
    fun `지역 그룹을 엔진에 전달하고 0건 재검색에서는 모두 해제한다`() {
        `when`(areas.resolveSearchPrefixes("서울")).thenReturn(listOf("11"))
        `when`(areas.resolveSearchPrefixes("강남구")).thenReturn(listOf("11680"))
        engineHits = {
            if (criteria.last().areaCodePrefixGroups.isEmpty()) {
                PlaceSearchHits(listOf(1), 1)
            } else {
                PlaceSearchHits(emptyList(), 0)
            }
        }

        val result = service.searchByName("서울 강남구 카페", null, 10, 99)

        assertEquals(listOf(1L), result.items.map { it.id })
        assertEquals(2, criteria.size)
        assertEquals(listOf(listOf("11"), listOf("11680")), criteria.first().areaCodePrefixGroups)
        assertTrue(criteria.last().areaCodePrefixGroups.isEmpty())
        assertTrue(criteria.last().categories.isEmpty())
        assertEquals(listOf("서울", "강남구", "카페"), criteria.last().textTokens)
        assertTrue(criteria.all { it.anchor == anchor.location })
    }

    @Test
    fun `기준 장소의 DB 좌표를 정렬 조건으로 전달하고 결과 순서를 보존한다`() {
        engineHits = { PlaceSearchHits(listOf(3, 1, 2), 3) }

        val result = service.searchByName("블루보틀", null, 10, 99)

        assertEquals(anchor.location, criteria.single().anchor)
        assertNull(criteria.single().viewport)
        assertEquals(listOf(3L, 1L, 2L), result.items.map { it.id })
    }

    @Test
    fun `존재하지 않는 기준 장소는 검색 전에 404로 거절한다`() {
        anchorExists = false

        val exception = assertThrows<BusinessException> { service.searchByName("카페", null, 10, 99) }

        assertEquals(PlaceErrorCode.PLACE_NOT_FOUND, exception.errorCode)
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun `엔진 장애는 DB 폴백 없이 503 으로 전파한다`() {
        engineHits = { throw BusinessException(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE) }

        val exception = assertThrows<BusinessException> { service.searchByName("블루보틀", null, 10, null) }

        assertEquals(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `엔진 후속 페이지 장애도 같은 커서로 재시도할 수 있게 503 을 반환한다`() {
        engineHits = { PlaceSearchHits(listOf(1), 3) }
        val first = service.searchByName("블루보틀", null, 1, null)
        engineHits = { throw BusinessException(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE) }

        val exception = assertThrows<BusinessException> { service.searchByName("블루보틀", first.nextCursor, 1, null) }

        assertEquals(503, exception.errorCode.status)
    }

    @Test
    fun `다음 페이지는 커서의 오프셋과 기준 장소를 이어간다`() {
        engineHits = { PlaceSearchHits(listOf(3), 3) }
        val first = service.searchByName("블루보틀", null, 1, 99)
        engineHits = { PlaceSearchHits(listOf(1), 3) }

        val second = service.searchByName("블루보틀", first.nextCursor, 1, 99)

        assertEquals(listOf(3L), first.items.map { it.id })
        assertEquals(listOf(1L), second.items.map { it.id })
        assertEquals(listOf(0, 1), criteria.map { it.from })
        assertTrue(criteria.all { it.anchor == anchor.location })
        assertTrue(second.hasNext)
    }

    @Test
    fun `다음 페이지에 기준 장소를 바꾸면 잘못된 커서로 거절한다`() {
        engineHits = { PlaceSearchHits(listOf(1), 3) }
        val first = service.searchByName("블루보틀", null, 1, 99)

        val exception = assertThrows<BusinessException> { service.searchByName("블루보틀", first.nextCursor, 1, 98) }

        assertEquals(400, exception.errorCode.status)
        assertEquals(1, criteria.size)
    }

    @Test
    fun `검색 윈도 마지막 불완전 페이지도 빠짐없이 조회한다`() {
        engineHits = { PlaceSearchHits(listOf(1), 10050) }
        val beforeLast = service.searchByName("블루보틀", PlaceSearchCursorCodec.encode(9980, false), 17, null)
        assertTrue(beforeLast.hasNext)
        val last = service.searchByName("블루보틀", beforeLast.nextCursor, 17, null)
        assertEquals(9997, criteria.last().from)
        assertEquals(3, criteria.last().size)
        assertFalse(last.hasNext)
    }

    private fun place(id: Long): Place =
        Place.reconstitute(
            id = id,
            status = PlaceStatus.ACTIVE,
            name = "블루보틀 $id",
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
