package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.AsyncConfig
import com.example.backend.place.application.event.PlaceBusinessHoursSyncRequestedEvent
import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 장소 저장·동기화 요청 이벤트를 커밋 후(AFTER_COMMIT) 받아 영업시간을 비동기로 동기화한다.
 * 외부 호출과 행 잠금이 따르므로 요청 스레드가 아닌 전용 실행기(동시 실행 제한)에서 돌린다. 미동기화 장소만 유스케이스가 걸러낸다.
 */
@Component
class PlaceBusinessHoursSyncListener(
    private val useCase: PlaceBusinessHoursSyncUseCase,
) {
    @Async(AsyncConfig.BUSINESS_HOURS_SYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPlacesSaved(event: PlacesSavedEvent) {
        useCase.sync(event.places.mapNotNull { it.id })
    }

    @Async(AsyncConfig.BUSINESS_HOURS_SYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSyncRequested(event: PlaceBusinessHoursSyncRequestedEvent) {
        useCase.sync(event.placeIds)
    }
}
