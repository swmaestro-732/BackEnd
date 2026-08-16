package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 장소 저장 이벤트를 커밋 후(AFTER_COMMIT) 검색 인덱스에 반영한다.
 * 롤백 시엔 색인하지 않는다(고스트 문서 방지). 색인은 커밋 순서대로 동기 처리 —
 * 요청 스레드에서 돌지만 단건 upsert 라 가볍고, 실패는 어댑터가 fail-soft(warn)로 삼킨다.
 * (응답 지연 최적화·완전 순서보장·실패 재시도는 outbox/큐 후속 — SCRUM-483.)
 */
@Component
class PlaceSearchSyncListener(
    private val port: PlaceSearchIndexPort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPlacesSaved(event: PlacesSavedEvent) {
        port.save(event.places)
    }
}
