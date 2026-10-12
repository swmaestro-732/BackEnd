package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import com.example.backend.place.application.port.outbound.BusinessHoursLookupPort
import com.example.backend.place.application.port.outbound.PlaceBusinessHourPersistencePort
import com.example.backend.place.domain.model.Place
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations

/**
 * 장소 영업시간 동기화. 외부 조회(HTTP)는 트랜잭션 밖에서 하고 DB 구간(대상 조회 / 행 교체)만 짧게 감싼다.
 * 조회 실패(null)면 동기화 시각을 남기지 않아 다음 기회에 재시도된다.
 */
@Service
class PlaceBusinessHoursSyncService(
    private val lookupPort: BusinessHoursLookupPort,
    private val persistencePort: PlaceBusinessHourPersistencePort,
    private val transactionOperations: TransactionOperations,
) : PlaceBusinessHoursSyncUseCase {
    override fun sync(placeIds: List<Long>): Int {
        if (placeIds.isEmpty()) return 0
        val targets = transactionOperations.execute { _ -> persistencePort.findUnsynced(placeIds) }.orEmpty()
        return syncEach(targets)
    }

    override fun backfillAll(): Int {
        var afterId: Long? = null
        var total = 0
        while (true) {
            val batch = transactionOperations.execute { _ -> persistencePort.findUnsynced(afterId, PAGE) }.orEmpty()
            if (batch.isEmpty()) break
            total += syncEach(batch)
            afterId = batch.last().id
            if (batch.size < PAGE) break
        }
        return total
    }

    private fun syncEach(places: List<Place>): Int =
        places.count { place ->
            val hours = lookupPort.findBusinessHours(place.name, place.address, place.location) ?: return@count false
            transactionOperations.execute { _ -> persistencePort.replaceBusinessHours(place.id!!, hours) }
            true
        }

    private companion object {
        const val PAGE = 100
    }
}
