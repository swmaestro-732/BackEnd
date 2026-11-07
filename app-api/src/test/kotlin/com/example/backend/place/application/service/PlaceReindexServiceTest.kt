package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaceReindexServiceTest {
    private fun place(id: Long): Place =
        Place.reconstitute(
            id = id,
            status = PlaceStatus.ACTIVE,
            name = "장소 $id",
            description = null,
            category = PlaceCategory.CAFE,
            location = Coordinate(latitude = 37.5, longitude = 127.0),
            address = "a",
            areaCode = null,
            imageUrl = null,
            businessStatus = PlaceBusinessStatus.UNKNOWN,
            kakaoPlaceId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
        )

    @Test
    fun `전체 장소를 페이지 단위로 한 번씩 색인하고 색인 수를 반환한다`() {
        val n = 1200
        val all = (1L..n).map { place(it) }

        var pageCalls = 0
        val fakePersistence =
            object : PlacePersistencePort {
                override fun findByKakaoIds(kakaoIds: List<String>): List<Place> = emptyList()

                override fun insertIgnoringConflicts(places: List<Place>) = Unit

                override fun findForIndex(
                    afterId: Long?,
                    limit: Int,
                ): List<Place> {
                    pageCalls++
                    return all.filter { it.id!! > (afterId ?: 0L) }.take(limit)
                }
            }

        val indexedIds = mutableListOf<Long>()
        val fakeSearchIndex =
            object : PlaceSearchIndexPort {
                override fun save(places: List<Place>) {
                    indexedIds += places.map { it.id!! }
                }
            }

        val total = PlaceReindexService(fakePersistence, fakeSearchIndex).reindexAll()

        assertEquals(n, total) { "색인 수 불일치" }
        assertEquals(all.map { it.id }, indexedIds) { "모든 id 를 정확히 한 번씩 색인하지 않음" }
        assertEquals(indexedIds.size, indexedIds.toSet().size) { "중복 색인 발생" }
        assertTrue(pageCalls > 2) { "페이징이 2페이지 초과로 돌지 않음: $pageCalls" }
    }
}
