package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.inbound.SavedPlaceSearchQueryPort
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import com.example.backend.user.application.port.inbound.dto.NearbySavedHit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class HomeNearbyPlaceAdapterTest {
    private val search = mock(SavedPlaceSearchQueryPort::class.java)
    private val savedPlaces = mock(SavedPlaceUseCase::class.java)
    private val adapter = HomeNearbyPlaceAdapter(search, savedPlaces)
    private val origin = Coordinate(37.544, 127.056)

    @Test
    fun `검색 결과가 없으면 방문 여부도 조회하지 않는다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2)).thenReturn(emptyList())

        assertTrue(adapter.findNearbySavedPlaces(7, origin, 2010.0, 2).isEmpty())
        verifyNoInteractions(savedPlaces)
    }

    @Test
    fun `표시 정보는 ES 결과를 그대로 쓰고 방문 여부만 DB 로 합쳐 거리순을 보존한다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2))
            .thenReturn(listOf(hit(20, 150.0), hit(10, 820.0)))
        `when`(savedPlaces.findVisited(7, listOf(20L, 10L))).thenReturn(mapOf(20L to true, 10L to false))

        val result = adapter.findNearbySavedPlaces(7, origin, 2010.0, 2)

        assertEquals(listOf(20L, 10L), result.map { it.placeId })
        assertEquals(listOf(150.0, 820.0), result.map { it.distanceMeters })
        assertEquals(listOf(true, false), result.map { it.visited })
        assertEquals("장소 20", result.first().name)
        assertEquals("https://img/20.jpg", result.first().imageUrl)
        assertEquals("CAFE", result.first().category)
        assertEquals(4.8, result.first().rating)
        assertEquals(212, result.first().ratingCount)
        verify(savedPlaces).findVisited(7, listOf(20L, 10L))
    }

    @Test
    fun `저장 레코드에 없는 placeId 의 방문 여부는 false 로 둔다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2)).thenReturn(listOf(hit(20, 150.0)))
        `when`(savedPlaces.findVisited(7, listOf(20L))).thenReturn(emptyMap())

        assertEquals(false, adapter.findNearbySavedPlaces(7, origin, 2010.0, 2).single().visited)
    }

    private fun hit(
        placeId: Long,
        distanceMeters: Double,
    ) = NearbySavedHit(
        placeId = placeId,
        distanceMeters = distanceMeters,
        name = "장소 $placeId",
        category = "CAFE",
        imageUrl = "https://img/$placeId.jpg",
        rating = 4.8,
        ratingCount = 212,
    )
}
