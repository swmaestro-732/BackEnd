package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import com.example.backend.place.application.port.outbound.BusinessHoursLookupPort
import com.example.backend.place.application.port.outbound.PlaceBusinessHourPersistencePort
import com.example.backend.place.domain.model.Place
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * 장소 영업시간 동기화. 외부 조회(HTTP)는 트랜잭션 밖에서 하고 DB 구간만 짧게 감싼다.
 * 조회 실패(null)면 동기화 시각을 남기지 않아 다음 기회에 재시도된다.
 *
 * 저장 직후(리스너)와 상세 조회(BFF) 양쪽에서 불린다. 조회 경로는 읽기 전용 트랜잭션 안이라
 * 쓰기 구간은 REQUIRES_NEW 로 독립시킨다.
 */
@Service
class PlaceBusinessHoursSyncService(
    private val lookupPort: BusinessHoursLookupPort,
    private val persistencePort: PlaceBusinessHourPersistencePort,
    transactionManager: PlatformTransactionManager,
) : PlaceBusinessHoursSyncUseCase {
    private val log = KotlinLogging.logger {}

    private val readTx = TransactionTemplate(transactionManager)
    private val writeTx =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    /** 같은 장소를 동시에 조회해도 외부 호출이 한 번만 나가게 막는다(인스턴스 내 한정). */
    private val locks = Array(LOCK_STRIPES) { ReentrantLock() }

    override fun sync(placeIds: List<Long>): Int {
        if (placeIds.isEmpty()) return 0
        val targets = readTx.execute { _ -> persistencePort.findUnsynced(placeIds) }.orEmpty()
        return targets.count { syncOne(it) }
    }

    /**
     * 장소 하나를 동기화한다. 장소별로 잠가 중복 외부 호출을 막고, 잠금을 얻은 뒤 한 번 더 확인해
     * 그사이 다른 요청이 채웠으면 건너뛴다. 실패는 이 장소에서 끝내고 호출부로 번지지 않는다.
     */
    private fun syncOne(place: Place): Boolean {
        val placeId = place.id!!
        val lock = locks[(placeId % LOCK_STRIPES).toInt()]
        if (!lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
            log.info { "장소 영업시간 동기화 대기 시간 초과로 건너뜀: placeId=$placeId" }
            return false
        }
        try {
            // 잠금 대기 중 다른 요청이 이미 채웠을 수 있다.
            if (readTx.execute { _ -> persistencePort.findUnsynced(listOf(placeId)) }.orEmpty().isEmpty()) return false
            val hours = lookupPort.findBusinessHours(place.name, place.address, place.location) ?: return false
            writeTx.execute { _ -> persistencePort.saveBusinessHours(placeId, hours) }
            return true
        } catch (exception: Exception) {
            log.warn(exception) { "장소 영업시간 저장 실패: placeId=$placeId" }
            return false
        } finally {
            lock.unlock()
        }
    }

    private companion object {
        const val LOCK_STRIPES = 64
        const val LOCK_WAIT_SECONDS = 3L
    }
}
