package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import com.example.backend.place.application.port.outbound.BusinessHoursLookupPort
import com.example.backend.place.application.port.outbound.PlaceBusinessHourPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

/**
 * 장소 영업시간 동기화. 장소별로 `places` 행을 잠근(FOR UPDATE SKIP LOCKED) 트랜잭션 안에서 외부를 한 번 조회하고 저장한다.
 * 잠금이 DB 에 있으므로 서버가 여러 대여도 같은 장소에 외부 호출은 한 번만 나가고, 못 잡은 쪽은 기다리지 않고 건너뛴다.
 * 조회 실패(null)면 동기화 시각을 남기지 않아 다음 기회에 재시도된다.
 *
 * 저장 직후(리스너)와 상세 조회(BFF) 양쪽에서 불린다. 조회 경로는 읽기 전용 트랜잭션 안이라
 * 잠금·저장 구간은 REQUIRES_NEW 로 독립시킨다. 행 잠금을 외부 호출 동안 유지해야 하므로 HTTP 호출이 그 트랜잭션 안에 들어간다.
 */
@Service
class PlaceBusinessHoursSyncService(
    private val lookupPort: BusinessHoursLookupPort,
    private val persistencePort: PlaceBusinessHourPersistencePort,
    transactionManager: PlatformTransactionManager,
) : PlaceBusinessHoursSyncUseCase {
    private val log = KotlinLogging.logger {}

    private val readTx = TransactionTemplate(transactionManager)
    private val lockTx =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    override fun sync(placeIds: List<Long>): Int {
        if (placeIds.isEmpty()) return 0
        val targets = readTx.execute { _ -> persistencePort.findUnsynced(placeIds) }.orEmpty()
        return targets.count { syncOne(it.id!!) }
    }

    /** 장소 하나를 동기화한다. 잠금을 못 얻거나 그사이 다른 쪽이 채웠으면 건너뛴다. 실패는 이 장소에서 끝내고 호출부로 번지지 않는다. */
    private fun syncOne(placeId: Long): Boolean =
        try {
            lockTx.execute { _ ->
                val place = persistencePort.lockUnsynced(placeId) ?: return@execute false
                val hours =
                    lookupPort.findBusinessHours(place.name, place.address, place.location) ?: return@execute false
                persistencePort.saveBusinessHours(placeId, hours)
                true
            } ?: false
        } catch (exception: Exception) {
            log.warn(exception) { "장소 영업시간 동기화 실패: placeId=$placeId" }
            false
        }
}
