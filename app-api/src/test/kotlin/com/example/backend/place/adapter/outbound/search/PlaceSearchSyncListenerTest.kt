package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.inbound.PlaceReindexUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * AFTER_COMMIT 리스너가 장소 저장 이벤트의 id 를 재색인 유스케이스로 위임하는지 검증한다.
 * 비동기·트랜잭션 없이 메서드를 직접 호출해 위임만 확인한다(색인 실동작은 OpenSearch 통합테스트가 담당).
 */
class PlaceSearchSyncListenerTest {
    private val reindexed = mutableListOf<List<Long>>()

    private val placeReindexUseCase =
        object : PlaceReindexUseCase {
            override fun reindexAll(): Int = error("리스너는 전체 재색인을 부르지 않는다")

            override fun reindex(placeIds: List<Long>) {
                reindexed += placeIds
            }
        }

    private val listener = PlaceSearchSyncListener(placeReindexUseCase)

    @Test
    fun `onPlacesSaved 는 장소 id 로 다시 읽어 색인하게 넘긴다`() {
        listener.onPlacesSaved(PlacesSavedEvent(listOf(1L, 2L)))

        assertEquals(listOf(listOf(1L, 2L)), reindexed)
    }
}
