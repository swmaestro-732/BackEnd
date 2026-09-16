package com.example.backend.place.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * AFTER_COMMIT 리스너가 장소 저장 이벤트를 검색 인덱스 포트의 save 로 위임하는지 검증한다.
 * 비동기·트랜잭션 없이 메서드를 직접 호출해 위임만 확인한다(색인 실동작은 OpenSearch 통합테스트가 담당).
 */
class PlaceSearchSyncListenerTest {
    private val savedBatches = mutableListOf<List<Place>>()

    private val port =
        object : PlaceSearchIndexPort {
            override fun save(places: List<Place>) {
                savedBatches += places
            }
        }

    private val listener = PlaceSearchSyncListener(port)

    @Test
    fun `onPlacesSaved 는 장소들을 포트에 저장한다`() {
        val places = listOf(place(1L), place(2L))

        listener.onPlacesSaved(PlacesSavedEvent(places))

        assertEquals(listOf(places), savedBatches)
    }

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
}
