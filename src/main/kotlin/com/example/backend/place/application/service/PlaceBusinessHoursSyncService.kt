package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import com.example.backend.place.application.port.outbound.BusinessHoursLookupPort
import com.example.backend.place.application.port.outbound.PlaceBusinessHourPersistencePort
import com.example.backend.place.domain.model.Place
import io.github.oshai.kotlinlogging.KotlinLogging
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
    private val log = KotlinLogging.logger {}

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

    /**
     * 장소 하나의 실패가 배치를 중단시키지 않도록 저장 구간도 장소별로 격리한다 —
     * 같은 장소를 동시에 동기화하면 행 교체(삭제→삽입)가 `(place_id, day_of_week)` 유니크와 충돌할 수 있다.
     */
    private fun syncEach(places: List<Place>): Int =
        places.count { place ->
            val hours = lookupPort.findBusinessHours(place.name, place.address, place.location) ?: return@count false
            try {
                transactionOperations.execute { _ -> persistencePort.replaceBusinessHours(place.id!!, hours) }
                true
            } catch (exception: Exception) {
                log.warn(exception) { "장소 영업시간 저장 실패: placeId=${place.id}" }
                false
            }
        }

    private companion object {
        const val PAGE = 100
    }
}
