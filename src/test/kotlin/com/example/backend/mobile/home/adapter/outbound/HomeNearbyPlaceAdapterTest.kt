package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.inbound.SavedPlaceSearchQueryPort
import com.example.backend.user.application.port.inbound.dto.NearbySavedHit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class HomeNearbyPlaceAdapterTest {
    private val search = mock(SavedPlaceSearchQueryPort::class.java)
    private val adapter = HomeNearbyPlaceAdapter(search)
    private val origin = Coordinate(37.544, 127.056)

    @Test
    fun `검색 결과가 없으면 빈 목록을 반환한다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2)).thenReturn(emptyList())

        assertTrue(adapter.findNearbySavedPlaces(7, origin, 2010.0, 2).isEmpty())
        verify(search).nearestSaved(7, origin, 2010.0, 2)
        verifyNoMoreInteractions(search)
    }

    @Test
    fun `표시 정보와 방문 여부는 색인 결과를 그대로 쓰고 거리순을 보존한다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2))
            .thenReturn(listOf(hit(20, 150.0, visited = true), hit(10, 820.0, visited = false)))

        val result = adapter.findNearbySavedPlaces(7, origin, 2010.0, 2)

        assertEquals(listOf(20L, 10L), result.map { it.placeId })
        assertEquals(listOf(150.0, 820.0), result.map { it.distanceMeters })
        assertEquals(listOf(true, false), result.map { it.visited })
        assertEquals("장소 20", result.first().name)
        assertEquals("https://img/20.jpg", result.first().imageUrl)
        assertEquals("CAFE", result.first().category)
        assertEquals(4.8, result.first().rating)
        assertEquals(212, result.first().ratingCount)
        verify(search).nearestSaved(7, origin, 2010.0, 2)
        verifyNoMoreInteractions(search)
    }

    private fun hit(
        placeId: Long,
        distanceMeters: Double,
        visited: Boolean,
    ) = NearbySavedHit(
        placeId = placeId,
        distanceMeters = distanceMeters,
        visited = visited,
        name = "장소 $placeId",
        category = "CAFE",
        imageUrl = "https://img/$placeId.jpg",
        rating = 4.8,
        ratingCount = 212,
    )
}
