package com.example.backend.user.adapter.outbound.search

import com.example.backend.user.application.event.SavedPlaceIndexedEvent
import com.example.backend.user.application.event.SavedPlaceRemovedEvent
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceIndexFallbackPort
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

/**
 * DB 커밋 후에만 저장 관계를 색인에 반영한다. 주 경로는 in-process 색인이고, 실패하면 그때만 SQS 폴백으로
 * 재처리를 넘긴다([SavedPlaceIndexFallbackPort] — 컨슈머가 같은 색인 포트로 멱등 반영). OS 가 ACID 를 보장하지 않으므로
 * DB 에 커밋된 저장이 색인에서 누락되지 않게 "동기 우선 + 실패 시 큐" 로 재처리를 보장한다(CourseCountEventHandler 선례).
 * 동시 트랜잭션 간 이벤트 순서는 보장하지 않는다(upsert/delete 멱등으로 흡수).
 *
 * 이벤트 저장소(Modulith)가 발행과 같은 트랜잭션에 이 리스너 몫의 기록을 남기고, 커밋 후 비동기로 부른다.
 * 동기 색인과 SQS 폴백이 모두 실패하면 예외가 올라와 기록이 미완료로 남고 재발행된다.
 */
@Component
class SavedPlaceSearchSyncListener(
    private val port: SavedPlaceSearchIndexPort,
    private val fallbackPort: SavedPlaceIndexFallbackPort,
) {
    private val log = KotlinLogging.logger {}

    @ApplicationModuleListener
    fun onSavedPlaceIndexed(event: SavedPlaceIndexedEvent) {
        val document =
            SavedPlaceIndexDocument(
                userId = event.userId,
                placeId = event.placeId,
                location = event.location,
                savedAt = event.savedAt,
                visited = event.visited,
                name = event.name,
                category = event.category,
                imageUrl = event.imageUrl,
                rating = event.rating,
                ratingCount = event.ratingCount,
                categoryDetail = event.categoryDetail,
            )
        try {
            port.index(document) // 동기 우선
        } catch (e: Exception) {
            log.warn { "동기 saved_place 색인 실패 → SQS 폴백: ${event.userId}:${event.placeId} — ${e.message}" }
            fallbackPort.enqueueIndex(document) // 실패 시 폴백 재처리
        }
    }

    @ApplicationModuleListener
    fun onSavedPlaceRemoved(event: SavedPlaceRemovedEvent) {
        try {
            port.delete(event.userId, event.placeId) // 동기 우선
        } catch (e: Exception) {
            log.warn { "동기 saved_place 삭제 실패 → SQS 폴백: ${event.userId}:${event.placeId} — ${e.message}" }
            fallbackPort.enqueueDelete(event.userId, event.placeId) // 실패 시 폴백 재처리
        }
    }
}
