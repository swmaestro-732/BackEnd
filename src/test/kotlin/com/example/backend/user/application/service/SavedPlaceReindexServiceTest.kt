package com.example.backend.user.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.outbound.PlaceAccessPort
import com.example.backend.user.application.port.outbound.PlaceRef
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlacePersistencePort
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import com.example.backend.user.domain.model.SavedPlace
import com.example.backend.user.domain.model.SavedPlaceCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class SavedPlaceReindexServiceTest {
    private val persistence = mock(SavedPlacePersistencePort::class.java)
    private val places = mock(PlaceAccessPort::class.java)
    private val batches = mutableListOf<List<SavedPlaceIndexDocument>>()
    private val index =
        object : SavedPlaceSearchIndexPort {
            override fun index(document: SavedPlaceIndexDocument) = error("재색인은 배치 포트를 사용해야 한다")

            override fun index(documents: List<SavedPlaceIndexDocument>) {
                batches += documents
            }

            override fun delete(
                userId: Long,
                placeId: Long,
            ) = error("재색인 중 문서를 삭제하면 안 된다")
        }
    private val service = SavedPlaceReindexService(persistence, places, index)
    private val location = Coordinate(latitude = 37.5, longitude = 127.0)
    private val savedAt = Instant.parse("2026-10-11T08:00:00Z")

    @Test
    fun `저장 관계가 없으면 장소 조회와 색인을 생략한다`() {
        `when`(persistence.findForIndex(null, 100)).thenReturn(emptyList())

        assertEquals(0, service.reindexAll())

        assertTrue(batches.isEmpty())
        verifyNoInteractions(places)
    }

    @Test
    fun `저장 관계를 페이지 단위로 좌표와 결합하고 마지막 행 id로 다음 페이지를 조회한다`() {
        val first = (1L..100L).map(::savedPlace)
        val second = listOf(savedPlace(101L), savedPlace(102L))
        `when`(persistence.findForIndex(null, 100)).thenReturn(first)
        `when`(persistence.findForIndex(100L, 100)).thenReturn(second)
        `when`(places.findPlaces((1L..100L).toList())).thenReturn((1L..100L).map(::place))
        `when`(places.findPlaces(listOf(101L, 102L))).thenReturn(listOf(place(102L), place(101L)))

        assertEquals(102, service.reindexAll())

        assertEquals(listOf(100, 2), batches.map { it.size })
        assertEquals((1L..102L).toList(), batches.flatten().map { it.placeId })
        assertEquals(
            listOf(
                SavedPlaceIndexDocument(7L, 101L, location, false, savedAt),
                SavedPlaceIndexDocument(7L, 102L, location, true, savedAt),
            ),
            batches.last(),
        )
        verify(persistence).findForIndex(null, 100)
        verify(persistence).findForIndex(100L, 100)
        verifyNoMoreInteractions(persistence)
    }

    @Test
    fun `마지막 페이지가 배치 크기와 같아도 빈 다음 페이지에서 종료한다`() {
        val first = (1L..100L).map(::savedPlace)
        `when`(persistence.findForIndex(null, 100)).thenReturn(first)
        `when`(persistence.findForIndex(100L, 100)).thenReturn(emptyList())
        `when`(places.findPlaces((1L..100L).toList())).thenReturn((1L..100L).map(::place))

        assertEquals(100, service.reindexAll())

        assertEquals(1, batches.size)
        verify(persistence).findForIndex(100L, 100)
    }

    @Test
    fun `존재하지 않는 장소의 저장은 건너뛰고 실제 색인 문서 수를 반환한다`() {
        val first = listOf(savedPlace(1L), savedPlace(2L), savedPlace(3L))
        `when`(persistence.findForIndex(null, 100)).thenReturn(first)
        `when`(places.findPlaces(listOf(1L, 2L, 3L))).thenReturn(listOf(place(1L), place(3L)))

        assertEquals(2, service.reindexAll())

        assertEquals(listOf(1L, 3L), batches.single().map { it.placeId })
    }

    @Test
    fun `한 페이지의 모든 장소가 없어도 원본 마지막 행 id로 다음 페이지를 진행한다`() {
        val first = (1L..100L).map(::savedPlace)
        `when`(persistence.findForIndex(null, 100)).thenReturn(first)
        `when`(persistence.findForIndex(100L, 100)).thenReturn(listOf(savedPlace(101L)))
        `when`(places.findPlaces((1L..100L).toList())).thenReturn(emptyList())
        `when`(places.findPlaces(listOf(101L))).thenReturn(listOf(place(101L)))

        assertEquals(1, service.reindexAll())

        assertEquals(listOf(listOf(SavedPlaceIndexDocument(7L, 101L, location, false, savedAt))), batches)
    }

    @Test
    fun `다른 사용자가 같은 장소를 저장했으면 좌표는 한 번 조회하고 관계는 각각 색인한다`() {
        val first = listOf(savedPlace(1L), savedPlace(2L).copy(userId = 8L, placeId = 1L))
        `when`(persistence.findForIndex(null, 100)).thenReturn(first)
        `when`(places.findPlaces(listOf(1L))).thenReturn(listOf(place(1L)))

        assertEquals(2, service.reindexAll())

        assertEquals(listOf(7L to 1L, 8L to 1L), batches.single().map { it.userId to it.placeId })
        verify(places).findPlaces(listOf(1L))
        verifyNoMoreInteractions(places)
    }

    private fun savedPlace(id: Long) =
        SavedPlace(
            id = id,
            userId = 7L,
            placeId = id,
            category = SavedPlaceCategory.CAFE,
            visited = id % 2 == 0L,
            savedAt = savedAt,
        )

    private fun place(id: Long) = PlaceRef(id, "CAFE", location)
}
