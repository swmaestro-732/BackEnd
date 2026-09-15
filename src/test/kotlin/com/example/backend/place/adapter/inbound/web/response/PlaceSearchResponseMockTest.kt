package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.inbound.dto.PlaceSummaryPage
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

    @Test
    fun `from() - hasNext=true 이면 nextCursor 가 마지막 장소의 id 이다`() {
        val items =
            listOf(
                PlaceSummary(
                    id = 1L,
                    name = "어니언 성수",
                    category = "CAFE",
                    imageUrl = "https://img.example.com/1.jpg",
                    latitude = 37.5445,
                    longitude = 127.0578,
                    address = "서울 성동구 성수이로7가길 11",
                    areaCode = null,
                ),
                PlaceSummary(
                    id = 5L,
                    name = "대림창고",
                    category = "CULTURE",
                    imageUrl = null,
                    latitude = 37.5418,
                    longitude = 127.0592,
                    address = "서울 성동구 성수동2가 333-1",
                    areaCode = "1120011400",
                ),
            )
        val page = PlaceSummaryPage(items = items, totalCount = 20, hasNext = true)

        val response = PlaceSearchResponse.from(page)

        assertThat(response.hasNext).isTrue
        assertThat(response.nextCursor).isEqualTo("5")
        assertThat(response.totalCount).isEqualTo(20)
        assertThat(response.places).hasSize(2)
        assertThat(response.places[0].id).isEqualTo(1L)
        assertThat(response.places[0].name).isEqualTo("어니언 성수")
        assertThat(response.places[0].imageUrl).isEqualTo("https://img.example.com/1.jpg")
        assertThat(response.places[0].categories).containsExactly("CAFE")
        assertThat(response.places[0].location.latitude).isEqualTo(37.5445)
        assertThat(response.places[0].location.longitude).isEqualTo(127.0578)
        assertThat(response.places[0].averageRating).isEqualTo(0.0)
        assertThat(response.places[0].reviewCount).isEqualTo(0)
        assertThat(response.places[0].walkingMinutes).isNull()
        assertThat(response.places[0].hasSaved).isFalse
    }

    @Test
    fun `from() - hasNext=false 이면 nextCursor 가 null 이다`() {
        val items =
            listOf(
                PlaceSummary(
                    id = 9L,
                    name = "센터커피 성수",
                    category = "CAFE",
                    imageUrl = null,
                    latitude = 37.5463,
                    longitude = 127.0537,
                    address = "서울 성동구 성수동2가 10-1",
                    areaCode = null,
                ),
            )
        val page = PlaceSummaryPage(items = items, totalCount = 1, hasNext = false)

        val response = PlaceSearchResponse.from(page)

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
        assertThat(response.places).hasSize(1)
        assertThat(response.places[0].id).isEqualTo(9L)
    }
}
