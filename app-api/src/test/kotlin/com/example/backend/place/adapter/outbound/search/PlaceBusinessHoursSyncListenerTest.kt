package com.example.backend.place.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.event.PlaceBusinessHoursSyncRequestedEvent
import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * AFTER_COMMIT 리스너가 저장된 장소 id·동기화 요청 id 를 동기화 유스케이스로 넘기는지 검증한다.
 * 비동기·트랜잭션 없이 메서드를 직접 호출해 위임만 확인한다.
 */
class PlaceBusinessHoursSyncListenerTest {
    private val requested = mutableListOf<List<Long>>()

    private val useCase =
        object : PlaceBusinessHoursSyncUseCase {
            override fun sync(placeIds: List<Long>): Int {
                requested += placeIds
                return placeIds.size
            }

            override fun requestSync(placeIds: List<Long>) = error("리스너는 requestSync 를 부르지 않는다")
        }

    private val listener = PlaceBusinessHoursSyncListener(useCase)

    @Test
    fun `onPlacesSaved 는 저장된 장소 id 를 동기화로 넘긴다`() {
        listener.onPlacesSaved(PlacesSavedEvent(listOf(place(1), place(2))))

        assertThat(requested).containsExactly(listOf(1L, 2L))
    }

    @Test
    fun `onSyncRequested 는 요청된 장소 id 를 동기화로 넘긴다`() {
        listener.onSyncRequested(PlaceBusinessHoursSyncRequestedEvent(listOf(7L)))

        assertThat(requested).containsExactly(listOf(7L))
    }

    @Test
    fun `빈 이벤트도 그대로 위임한다`() {
        listener.onPlacesSaved(PlacesSavedEvent(emptyList()))

        assertThat(requested).containsExactly(emptyList<Long>())
    }

    private fun place(id: Long) =
        Place.reconstitute(
            id = id,
            status = PlaceStatus.ACTIVE,
            name = "장소$id",
            description = null,
            category = PlaceCategory.CAFE,
            location = Coordinate(latitude = 37.5, longitude = 127.0),
            address = "서울시 어딘가 $id",
            imageUrl = null,
            businessStatus = PlaceBusinessStatus.OPEN,
            kakaoPlaceId = "kakao-$id",
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
        )
}
