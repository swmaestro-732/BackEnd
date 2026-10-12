package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 장소 저장 이벤트를 커밋 후(AFTER_COMMIT) 받아 영업시간을 비동기로 동기화한다.
 * 장소당 외부 호출이 두 번이라 요청 스레드에서 돌리지 않는다. 미동기화 장소만 유스케이스가 걸러낸다.
 */
@Component
class PlaceBusinessHoursSyncListener(
    private val useCase: PlaceBusinessHoursSyncUseCase,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPlacesSaved(event: PlacesSavedEvent) {
        useCase.sync(event.places.mapNotNull { it.id })
    }
}
