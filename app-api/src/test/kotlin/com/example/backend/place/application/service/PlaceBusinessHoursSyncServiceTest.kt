package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.event.PlaceBusinessHoursSyncRequestedEvent
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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus

/**
 * 영업시간 동기화 — 미동기화 선별, 조회 실패 시 시각 미기록(재시도), 저장 실패 격리,
 * 잠금을 못 얻은 장소 건너뛰기, 동기화 요청 이벤트 발행을 검증한다.
 * 실 DB·HTTP 대신 페이크로 서비스 로직만 좁혀 본다(행 잠금 자체는 리포지토리 테스트).
 */
class PlaceBusinessHoursSyncServiceTest {
    private val hours = listOf(PlaceBusinessHour(1, LocalTime(9, 0), LocalTime(22, 0)))
    private val published = mutableListOf<Any>()
    private val eventPublisher = ApplicationEventPublisher { published += it }

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
    fun `잠금을 얻지 못한 장소는 외부를 부르지 않고 건너뛴다`() {
        // 2 는 다른 서버가 잠근 상태 — 미동기화 목록에는 있지만 잠금이 null 로 돌아온다
        val persistence = FakePersistence(unsynced = listOf(place(1), place(2)), lockedElsewhere = setOf(2L))
        val lookup = FakeLookup { hours }

        assertThat(service(lookup, persistence).sync(listOf(1L, 2L))).isEqualTo(1)
        assertThat(lookup.calls).containsExactly("장소1")
        assertThat(persistence.saved.keys).containsExactly(1L)
    }

    @Test
    fun `requestSync 는 미동기화 장소만 이벤트로 발행하고 외부를 부르지 않는다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1)))
        val lookup = FakeLookup { hours }

        service(lookup, persistence).requestSync(listOf(1L, 3L))

        assertThat(published).containsExactly(PlaceBusinessHoursSyncRequestedEvent(listOf(1L)))
        assertThat(lookup.calls).isEmpty()
        assertThat(persistence.saved).isEmpty()
    }

    @Test
    fun `requestSync 는 모두 동기화된 장소면 이벤트를 발행하지 않는다`() {
        service(FakeLookup { hours }, FakePersistence()).requestSync(listOf(3L))

        assertThat(published).isEmpty()
    }

    private fun service(
        lookup: BusinessHoursLookupPort,
        persistence: PlaceBusinessHourPersistencePort,
    ) = PlaceBusinessHoursSyncService(lookup, persistence, eventPublisher, NoopTransactionManager())

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

    /**
     * 저장되면 동기화된 것으로 보고 이후 findUnsynced·lockUnsynced 대상에서 빠진다.
     * lockedElsewhere 의 장소는 잠금에 실패(null)하고, failOn 의 장소는 저장에서 예외를 던진다.
     */
    private class FakePersistence(
        unsynced: List<Place> = emptyList(),
        private val lockedElsewhere: Set<Long> = emptySet(),
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

        override fun lockUnsynced(placeId: Long): Place? =
            synchronized(this) { pending[placeId]?.takeUnless { placeId in lockedElsewhere } }

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
