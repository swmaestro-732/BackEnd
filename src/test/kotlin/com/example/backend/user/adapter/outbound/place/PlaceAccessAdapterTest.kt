package com.example.backend.user.adapter.outbound.place

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.user.application.port.outbound.PlaceRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PlaceAccessAdapterTest {
    private val query = mock(PlaceQueryUseCase::class.java)
    private val adapter = PlaceAccessAdapter(query)

    @Test
    fun `장소 조회는 카테고리와 위경도를 user 소유 DTO로 변환한다`() {
        `when`(query.findPlacesById(listOf(42L))).thenReturn(listOf(place(42L, 37.51, 127.02)))

        assertEquals(PlaceRef(42L, "CAFE", Coordinate(37.51, 127.02)), adapter.findPlace(42L))
        verify(query).findPlacesById(listOf(42L))
    }

    @Test
    fun `삭제되거나 존재하지 않는 장소는 null을 반환한다`() {
        `when`(query.findPlacesById(listOf(42L))).thenReturn(emptyList())

        assertNull(adapter.findPlace(42L))
    }

    @Test
    fun `재색인용 장소 배치는 한 번 조회해 각 장소의 좌표를 유지한다`() {
        `when`(query.findPlacesById(listOf(42L, 43L, 44L)))
            .thenReturn(listOf(place(44L, 37.53, 127.04), place(42L, 37.51, 127.02)))

        assertEquals(
            listOf(
                PlaceRef(44L, "CAFE", Coordinate(37.53, 127.04)),
                PlaceRef(42L, "CAFE", Coordinate(37.51, 127.02)),
            ),
            adapter.findPlaces(listOf(42L, 43L, 44L)),
        )
        verify(query).findPlacesById(listOf(42L, 43L, 44L))
    }

    @Test
    fun `빈 배치는 빈 목록을 반환한다`() {
        assertEquals(emptyList<PlaceRef>(), adapter.findPlaces(emptyList()))
    }

    private fun place(
        id: Long,
        latitude: Double,
        longitude: Double,
    ) = PlaceSummary(id, "장소 $id", "CAFE", null, latitude, longitude, "주소", null)
}
