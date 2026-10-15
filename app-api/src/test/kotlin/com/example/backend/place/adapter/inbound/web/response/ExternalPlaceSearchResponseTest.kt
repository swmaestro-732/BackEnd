package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.application.port.inbound.dto.PlaceSearchResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExternalPlaceSearchResponseTest {
    @Test
    fun `from() — PlaceSearchResult 목록을 응답으로 변환한다`() {
        val results =
            listOf(
                PlaceSearchResult(
                    id = 101L,
                    name = "어니언 성수",
                    category = "CAFE",
                    roadAddress = "서울 성동구 성수이로7가길 11",
                    address = "서울 성동구 성수동2가 123",
                    latitude = 37.5445,
                    longitude = 127.0578,
                    telephone = "02-1234-5678",
                ),
                PlaceSearchResult(
                    id = 102L,
                    name = "대림창고",
                    category = "CAFE",
                    roadAddress = null,
                    address = null,
                    latitude = 37.5500,
                    longitude = 127.0600,
                    telephone = null,
                ),
            )

        val response = ExternalPlaceSearchResponse.from(results)

        assertEquals(2, response.places.size)

        val first = response.places[0]
        assertEquals(101L, first.id)
        assertEquals("어니언 성수", first.name)
        assertEquals("CAFE", first.category)
        assertEquals("서울 성동구 성수이로7가길 11", first.roadAddress)
        assertEquals(37.5445, first.latitude)
        assertEquals(127.0578, first.longitude)
        assertEquals("02-1234-5678", first.telephone)

        val second = response.places[1]
        assertEquals(102L, second.id)
        assertNull(second.roadAddress)
        assertNull(second.address)
        assertNull(second.telephone)
    }

    @Test
    fun `from() — 빈 목록은 빈 응답을 반환한다`() {
        val response = ExternalPlaceSearchResponse.from(emptyList())

        assertTrue(response.places.isEmpty())
    }

    @Test
    fun `ExternalPlaceSearchResponse Item equals — 같은 값이면 동일하다`() {
        val a = ExternalPlaceSearchResponse.Item(1L, "장소", "CAFE", null, null, 37.0, 127.0, null)
        val b = ExternalPlaceSearchResponse.Item(1L, "장소", "CAFE", null, null, 37.0, 127.0, null)

        assertEquals(a, b)
    }
}
