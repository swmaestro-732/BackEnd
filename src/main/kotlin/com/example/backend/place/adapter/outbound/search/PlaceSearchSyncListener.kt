package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 장소 저장 이벤트를 커밋 후(AFTER_COMMIT) 비동기로 검색 인덱스에 반영한다.
 * 요청/트랜잭션 스레드를 막지 않고, 롤백 시엔 색인하지 않는다(고스트 문서 방지).
 */
@Component
class PlaceSearchSyncListener(
    private val port: PlaceSearchIndexPort,
) {
    @Async("searchIndexExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPlacesSaved(event: PlacesSavedEvent) {
        // TODO(SQS): 실패 시 재처리 큐로 폴백 — 현재는 어댑터가 fail-soft(warn) 처리.
        port.save(event.places)
    }
}
