package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
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
    private val places = mock(PlaceQueryUseCase::class.java)
    private val adapter = HomeNearbyPlaceAdapter(search, savedPlaces, places)
    private val origin = Coordinate(37.544, 127.056)

    @Test
    fun `검색 결과가 없으면 DB 를 조회하지 않는다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2)).thenReturn(emptyList())

        assertTrue(adapter.findNearbySavedPlaces(7, origin, 2010.0, 2).isEmpty())
        verifyNoInteractions(places)
        verifyNoInteractions(savedPlaces)
    }

    @Test
    fun `OS 결과 두 개만 DB 로 조회하고 거리순 방문 여부와 최신 상세를 합친다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2))
            .thenReturn(listOf(NearbySavedHit(20, 150.0), NearbySavedHit(10, 820.0)))
        `when`(places.findPlacesById(listOf(20L, 10L))).thenReturn(listOf(place(10), place(20)))
        `when`(savedPlaces.findVisited(7, listOf(20L, 10L))).thenReturn(mapOf(20L to true, 10L to false))

        val result = adapter.findNearbySavedPlaces(7, origin, 2010.0, 2)

        assertEquals(listOf(20L, 10L), result.map { it.placeId })
        assertEquals(listOf(true, false), result.map { it.visited })
        assertEquals(listOf(150.0, 820.0), result.map { it.distanceMeters })
        assertEquals("장소 20", result.first().name)
        assertEquals("https://img/20.jpg", result.first().imageUrl)
        assertEquals("CAFE", result.first().category)
        assertEquals(4.8, result.first().rating)
        assertEquals(212, result.first().ratingCount)
        verify(places).findPlacesById(listOf(20L, 10L))
        verify(savedPlaces).findVisited(7, listOf(20L, 10L))
    }

    @Test
    fun `저장 레코드에 없는 placeId 의 방문 여부는 false 로 둔다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2)).thenReturn(listOf(NearbySavedHit(20, 150.0)))
        `when`(places.findPlacesById(listOf(20L))).thenReturn(listOf(place(20)))
        `when`(savedPlaces.findVisited(7, listOf(20L))).thenReturn(emptyMap())

        val result = adapter.findNearbySavedPlaces(7, origin, 2010.0, 2)

        assertEquals(false, result.single().visited)
    }

    @Test
    fun `DB 에서 삭제된 장소는 제외하고 나머지 OS 순서를 유지한다`() {
        `when`(search.nearestSaved(7, origin, 2010.0, 2))
            .thenReturn(listOf(NearbySavedHit(20, 150.0), NearbySavedHit(10, 820.0)))
        `when`(places.findPlacesById(listOf(20L, 10L))).thenReturn(listOf(place(10)))
        `when`(savedPlaces.findVisited(7, listOf(20L, 10L))).thenReturn(mapOf(10L to false))

        val result = adapter.findNearbySavedPlaces(7, origin, 2010.0, 2)

        assertEquals(listOf(10L), result.map { it.placeId })
        assertEquals(820.0, result.single().distanceMeters)
        assertEquals(false, result.single().visited)
    }

    private fun place(id: Long) =
        PlaceSummary(id, "장소 $id", "CAFE", "https://img/$id.jpg", 37.544, 127.056, "서울", null, 4.8, 212)
}
