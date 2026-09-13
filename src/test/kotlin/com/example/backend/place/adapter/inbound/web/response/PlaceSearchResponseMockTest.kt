package com.example.backend.place.adapter.inbound.web.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlaceSearchResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = PlaceSearchResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() totalCount 는 places 개수와 일치한다`() {
        val response = PlaceSearchResponse.mock()

        assertThat(response.totalCount).isEqualTo(response.places.size)
    }

    @Test
    fun `mock() hasNext 는 false 이고 nextCursor 는 null 이다`() {
        val response = PlaceSearchResponse.mock()

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
    }

    @Test
    fun `mock() 각 장소는 id·name·location 이 채워져 있다`() {
        val response = PlaceSearchResponse.mock()

        response.places.forEach { place ->
            assertThat(place.id).isPositive
            assertThat(place.name).isNotBlank
            assertThat(place.categories).isNotEmpty
            assertThat(place.location.latitude).isBetween(-90.0, 90.0)
            assertThat(place.location.longitude).isBetween(-180.0, 180.0)
        }
    }

    @Test
    fun `mock() 장소 목록은 비어 있지 않다`() {
        val response = PlaceSearchResponse.mock()

        assertThat(response.places).isNotEmpty
    }
}
