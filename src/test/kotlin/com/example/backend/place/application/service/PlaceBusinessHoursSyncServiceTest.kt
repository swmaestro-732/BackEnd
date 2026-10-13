package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.BusinessHoursLookupPort
import com.example.backend.place.application.port.outbound.PlaceBusinessHourPersistencePort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessHour
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import kotlinx.datetime.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 영업시간 동기화 — 미동기화 선별, 조회 실패 시 시각 미기록(재시도), 저장 실패 격리,
 * 같은 장소 동시 요청의 중복 외부 호출 차단을 검증한다. 실 DB·HTTP 대신 페이크로 서비스 로직만 좁혀 본다.
 */
class PlaceBusinessHoursSyncServiceTest {
    private val hours = listOf(PlaceBusinessHour(1, LocalTime(9, 0), LocalTime(22, 0)))

    @Test
    fun `미동기화 장소만 조회해 저장한다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1), place(2)))
        val lookup = FakeLookup { hours }

        // 3 은 이미 동기화된 장소라 페이크가 대상에서 제외한다
        assertThat(service(lookup, persistence).sync(listOf(1L, 2L, 3L))).isEqualTo(2)
        assertThat(persistence.saved.keys).containsExactlyInAnyOrder(1L, 2L)
        assertThat(lookup.calls).containsExactlyInAnyOrder("장소1", "장소2")
    }

    @Test
    fun `빈 id 목록은 조회하지 않는다`() {
        val persistence = FakePersistence()

        assertThat(service(FakeLookup { hours }, persistence).sync(emptyList())).isEqualTo(0)
        assertThat(persistence.unsyncedCalls).isEqualTo(0)
    }

    @Test
    fun `외부 조회가 실패하면 저장하지 않아 다음 기회에 재시도된다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1)))

        assertThat(service(FakeLookup { null }, persistence).sync(listOf(1L))).isEqualTo(0)
        assertThat(persistence.saved).isEmpty()
    }

    @Test
    fun `영업시간이 없는 장소는 빈 목록으로 기록해 재조회하지 않는다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1)))

        assertThat(service(FakeLookup { emptyList() }, persistence).sync(listOf(1L))).isEqualTo(1)
        assertThat(persistence.saved).containsEntry(1L, emptyList<PlaceBusinessHour>())
    }

    @Test
    fun `한 장소의 저장 실패가 나머지 장소 처리를 막지 않는다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1), place(2), place(3)), failOn = setOf(2L))

        assertThat(service(FakeLookup { hours }, persistence).sync(listOf(1L, 2L, 3L))).isEqualTo(2)
        assertThat(persistence.saved.keys).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    fun `같은 장소를 동시에 요청해도 외부 호출은 한 번만 나간다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1)))
        val started = CountDownLatch(1)
        // 첫 호출이 잠금을 쥔 채 머무는 동안 다른 스레드가 같은 장소로 들어오게 한다.
        val lookup =
            FakeLookup {
                started.countDown()
                Thread.sleep(300)
                hours
            }
        val svc = service(lookup, persistence)

        val pool = Executors.newFixedThreadPool(4)
        try {
            val tasks = (1..4).map { pool.submit<Int> { svc.sync(listOf(1L)) } }
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
            val synced = tasks.sumOf { it.get(10, TimeUnit.SECONDS) }

            // 잠금을 얻은 한 스레드만 외부를 부르고, 나머지는 재확인에서 이미 채워진 것을 보고 건너뛴다.
            assertThat(lookup.calls).hasSize(1)
            assertThat(synced).isEqualTo(1)
            assertThat(persistence.saved.keys).containsExactly(1L)
        } finally {
            pool.shutdownNow()
        }
    }

    private fun service(
        lookup: BusinessHoursLookupPort,
        persistence: PlaceBusinessHourPersistencePort,
    ) = PlaceBusinessHoursSyncService(lookup, persistence, NoopTransactionManager())

    private fun place(id: Long) =
        Place.reconstitute(
            id = id,
            status = PlaceStatus.ACTIVE,
            name = "장소$id",
            description = null,
            category = PlaceCategory.CAFE,
            location = Coordinate(latitude = 37.5, longitude = 127.0),
            address = "서울시 어딘가 $id",
            imageUrl = null,
            businessStatus = PlaceBusinessStatus.OPEN,
            kakaoPlaceId = "kakao-$id",
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
        )

    /** 단위 테스트에는 실 트랜잭션이 없으므로 콜백만 실행시킨다. */
    private class NoopTransactionManager : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()

        override fun commit(status: TransactionStatus) = Unit

        override fun rollback(status: TransactionStatus) = Unit
    }

    private class FakeLookup(
        private val answer: () -> List<PlaceBusinessHour>?,
    ) : BusinessHoursLookupPort {
        private val recorded = mutableListOf<String>()
        val calls: List<String> get() = synchronized(recorded) { recorded.toList() }

        override fun findBusinessHours(
            name: String,
            address: String,
            coordinate: Coordinate,
        ): List<PlaceBusinessHour>? {
            synchronized(recorded) { recorded += name }
            return answer()
        }
    }

    /** 저장되면 동기화된 것으로 보고 이후 findUnsynced 대상에서 빠진다. failOn 의 장소는 저장에서 예외를 던진다. */
    private class FakePersistence(
        unsynced: List<Place> = emptyList(),
        private val failOn: Set<Long> = emptySet(),
    ) : PlaceBusinessHourPersistencePort {
        private val pending = unsynced.associateBy { it.id!! }.toMutableMap()
        val saved = mutableMapOf<Long, List<PlaceBusinessHour>>()

        @Volatile
        var unsyncedCalls = 0

        override fun findUnsynced(placeIds: List<Long>): List<Place> =
            synchronized(this) {
                unsyncedCalls++
                placeIds.mapNotNull { pending[it] }
            }

        override fun saveBusinessHours(
            placeId: Long,
            hours: List<PlaceBusinessHour>,
        ) {
            if (placeId in failOn) error("저장 실패 모사: placeId=$placeId")
            synchronized(this) {
                saved[placeId] = hours
                pending.remove(placeId)
            }
        }
    }
}
