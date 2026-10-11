package com.example.backend.user.adapter.outbound.search

import com.example.backend.user.application.event.SavedPlaceIndexedEvent
import com.example.backend.user.application.event.SavedPlaceRemovedEvent
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * DB 커밋 후에만 저장 관계를 동기 반영한다. 실패는 색인 어댑터에서 흡수한다.
 * CourseSearchSyncListener 와 동일하게 동시 트랜잭션 간 이벤트 순서·재시도는 보장하지 않는다.
 */
@Component
class SavedPlaceSearchSyncListener(
    private val port: SavedPlaceSearchIndexPort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSavedPlaceIndexed(event: SavedPlaceIndexedEvent) {
        port.index(
            SavedPlaceIndexDocument(event.userId, event.placeId, event.location, event.visited, event.savedAt),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSavedPlaceRemoved(event: SavedPlaceRemovedEvent) {
        port.delete(event.userId, event.placeId)
    }
}
