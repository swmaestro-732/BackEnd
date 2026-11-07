package com.example.backend.user.adapter.outbound.search

import com.example.backend.user.application.event.SavedPlaceIndexedEvent
import com.example.backend.user.application.event.SavedPlaceRemovedEvent
import com.example.backend.user.application.port.inbound.SavedPlaceReindexUseCase
import com.example.backend.user.application.port.outbound.SavedPlaceIndexFallbackPort
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation

/**
 * DB 커밋 후에만 저장 관계를 색인에 반영한다. 저장, 취소 이벤트 모두 신호로만 쓰고, 처리할 때 DB 의 현재 상태를 다시 읽어
 * 저장돼 있으면 upsert, 아니면 delete 한다. 그래서 저장과 취소가 엇갈려 도착하거나 오래된 기록이 재발행돼도 색인은 DB 와 같아진다.
 *
 * 주 경로는 in-process 색인이고, 실패하면 그때만 SQS 폴백으로 재처리를 넘긴다([SavedPlaceIndexFallbackPort]).
 * 메시지에는 처리 시점에 정한 동작(upsert 문서 또는 delete 키)을 싣고, 컨슈머가 같은 색인 포트로 멱등 반영한다.
 *
 * 이벤트 저장소(Modulith)가 발행과 같은 트랜잭션에 이 리스너 몫의 기록을 남기고, 커밋 후 비동기로 부른다.
 * DB 읽기나 SQS 폴백이 실패하면 예외가 올라와 기록이 미완료로 남고 재발행된다.
 * 리스너 자체는 트랜잭션을 열지 않는다(NOT_SUPPORTED). DB 는 [SavedPlaceReindexUseCase.findDocument] 의 짧은 읽기
 * 트랜잭션에서만 읽어 OpenSearch 호출 동안 DB 연결을 잡지 않는다.
 */
@Component
class SavedPlaceSearchSyncListener(
    private val port: SavedPlaceSearchIndexPort,
    private val fallbackPort: SavedPlaceIndexFallbackPort,
    private val savedPlaceReindexUseCase: SavedPlaceReindexUseCase,
) {
    private val log = KotlinLogging.logger {}

    @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
    fun onSavedPlaceIndexed(event: SavedPlaceIndexedEvent) = sync(event.userId, event.placeId)

    @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
    fun onSavedPlaceRemoved(event: SavedPlaceRemovedEvent) = sync(event.userId, event.placeId)

    private fun sync(
        userId: Long,
        placeId: Long,
    ) {
        val document = savedPlaceReindexUseCase.findDocument(userId, placeId)
        try {
            if (document != null) port.index(document) else port.delete(userId, placeId) // 동기 우선
        } catch (e: Exception) {
            log.warn { "동기 saved_place 색인 반영 실패, SQS 폴백: $userId:$placeId (${e.message})" }
            if (document != null) fallbackPort.enqueueIndex(document) else fallbackPort.enqueueDelete(userId, placeId)
        }
    }
}
