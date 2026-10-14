package com.example.backend.user.adapter.outbound.search

import com.example.backend.user.application.event.SavedPlaceIndexedEvent
import com.example.backend.user.application.event.SavedPlaceRemovedEvent
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceIndexFallbackPort
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * DB 커밋 후에만 저장 관계를 동기 반영한다. 주 경로는 동기(in-process) 색인이고, 실패하면 그때만 SQS 폴백으로
 * 재처리를 넘긴다([SavedPlaceIndexFallbackPort] — 컨슈머가 같은 색인 포트로 멱등 반영). OS 가 ACID 를 보장하지 않으므로
 * DB 에 커밋된 저장이 색인에서 누락되지 않게 "동기 우선 + 실패 시 큐" 로 재처리를 보장한다(CourseCountEventHandler 선례).
 * 동시 트랜잭션 간 이벤트 순서는 보장하지 않는다(upsert/delete 멱등으로 흡수).
 */
@Component
class SavedPlaceSearchSyncListener(
    private val port: SavedPlaceSearchIndexPort,
    private val fallbackPort: SavedPlaceIndexFallbackPort,
) {
    private val log = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSavedPlaceIndexed(event: SavedPlaceIndexedEvent) {
        val document =
            SavedPlaceIndexDocument(event.userId, event.placeId, event.location, event.savedAt)
        try {
            port.index(document) // 동기 우선
        } catch (e: Exception) {
            log.warn { "동기 saved_place 색인 실패 → SQS 폴백: ${event.userId}:${event.placeId} — ${e.message}" }
            fallbackPort.enqueueIndex(document) // 실패 시 폴백 재처리
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSavedPlaceRemoved(event: SavedPlaceRemovedEvent) {
        try {
            port.delete(event.userId, event.placeId) // 동기 우선
        } catch (e: Exception) {
            log.warn { "동기 saved_place 삭제 실패 → SQS 폴백: ${event.userId}:${event.placeId} — ${e.message}" }
            fallbackPort.enqueueDelete(event.userId, event.placeId) // 실패 시 폴백 재처리
        }
    }
}
