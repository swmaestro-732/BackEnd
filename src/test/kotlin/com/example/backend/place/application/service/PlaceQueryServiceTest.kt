package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlaceQueryServiceTest {
    private var portSearchResult: List<Place> = emptyList()
    private var portFindResult: List<Place> = emptyList()
    private var portCount: Long = 0L
    private var capturedSearchLimit: Int? = null

    private val port =
        object : PlaceQueryPort {
            override fun findPlacesById(placeIds: List<Long>): List<Place> = portFindResult

            override fun searchByName(
                query: String,
                cursor: String?,
                limit: Int,
            ): List<Place> {
                capturedSearchLimit = limit
                return portSearchResult
            }

            override fun countByName(query: String): Long = portCount
        }

    private val service = PlaceQueryService(port)

    @Test
    fun `searchByName - 빈 검색어는 포트를 호출하지 않고 빈 페이지를 반환한다`() {
        portSearchResult = listOf(makePlace(1L)) // 포트가 호출되면 이 값이 반환되겠지만 호출 안 됨

        val page = service.searchByName("", null, 10)

        assertThat(page.items).isEmpty()
        assertThat(page.totalCount).isEqualTo(0)
        assertThat(page.hasNext).isFalse
    }

    @Test
    fun `searchByName - 공백만 있는 검색어는 빈 페이지를 반환한다`() {
        val page = service.searchByName("   ", null, 10)

        assertThat(page.items).isEmpty()
    }

    @Test
    fun `searchByName - 결과가 size 이하이면 hasNext=false 이다`() {
        portSearchResult = List(3) { makePlace(it.toLong() + 1) }
        portCount = 3L

        val page = service.searchByName("카페", null, 10)

        assertThat(capturedSearchLimit).isEqualTo(11) // service passes size+1
        assertThat(page.hasNext).isFalse
        assertThat(page.items).hasSize(3)
        assertThat(page.totalCount).isEqualTo(3)
    }

    @Test
    fun `searchByName - 결과가 size+1 개이면 hasNext=true 이고 items 는 size 개다`() {
        portSearchResult = List(11) { makePlace(it.toLong() + 1) }
        portCount = 15L

        val page = service.searchByName("카페", null, 10)

        assertThat(page.hasNext).isTrue
        assertThat(page.items).hasSize(10)
        assertThat(page.totalCount).isEqualTo(15)
    }

    @Test
    fun `searchByName - PlaceSummary 매핑 — id·name·category·imageUrl·좌표가 옮겨진다`() {
        portSearchResult = listOf(makePlace(42L, "어니언 성수", "CAFE", "https://img.example.com/1.jpg"))
        portCount = 1L

        val page = service.searchByName("어니언", null, 10)

        assertThat(page.items).hasSize(1)
        val item = page.items[0]
        assertThat(item.id).isEqualTo(42L)
        assertThat(item.name).isEqualTo("어니언 성수")
        assertThat(item.category).isEqualTo("CAFE")
        assertThat(item.imageUrl).isEqualTo("https://img.example.com/1.jpg")
        assertThat(item.latitude).isEqualTo(37.5)
        assertThat(item.longitude).isEqualTo(127.0)
    }

    @Test
    fun `findPlacesById - 빈 목록은 포트 호출 없이 빈 리스트를 반환한다`() {
        portFindResult = listOf(makePlace(99L)) // 호출되면 반환될 값

        val result = service.findPlacesById(emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun `findPlacesById - 비어 있지 않은 목록은 PlaceSummary 로 매핑된다`() {
        portFindResult = listOf(makePlace(7L, "대림창고", "CULTURE"))

        val result = service.findPlacesById(listOf(7L))

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(7L)
        assertThat(result[0].name).isEqualTo("대림창고")
        assertThat(result[0].category).isEqualTo("CULTURE")
    }

    private fun makePlace(
        id: Long,
        name: String = "장소$id",
        category: String = "CAFE",
        imageUrl: String? = null,
    ): Place =
        Place.reconstitute(
            id = id,
            status = PlaceStatus.ACTIVE,
            name = name,
            description = null,
            category = PlaceCategory.valueOf(category),
            location = Coordinate(37.5, 127.0),
            address = "서울시 성동구",
            areaCode = null,
            imageUrl = imageUrl,
            businessStatus = PlaceBusinessStatus.UNKNOWN,
            kakaoPlaceId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
        )
}
