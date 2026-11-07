package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 장소 저장 이벤트를 커밋 후(AFTER_COMMIT) 검색 인덱스에 동기 반영한다.
 * 롤백 시엔 색인하지 않는다(고스트 문서 방지). 요청 스레드에서 돌지만 place 는 카카오 검색분(최대 ~45건)이라
 * bulk 가 작고, 실패는 어댑터가 fail-soft(warn)로 삼킨다.
 * 단, 서로 다른 트랜잭션(동시 요청)의 전역 순서는 보장하지 않는다. 완전 순서보장(버전/시퀀스 검사)·
 * 응답 지연 최적화·실패 재시도·대량 색인 분리는 outbox/큐 후속에서 다룬다 — SCRUM-483.
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
