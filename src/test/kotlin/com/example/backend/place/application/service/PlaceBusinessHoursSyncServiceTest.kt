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
import org.springframework.transaction.support.TransactionOperations

/**
 * 영업시간 동기화 — 미동기화 선별, 조회 실패 시 시각 미기록(재시도), 저장 실패 격리, 백필 페이징을 검증한다.
 * 실 DB·HTTP 대신 in-memory 페이크 포트로 서비스 로직만 좁혀 확인한다.
 */
class PlaceBusinessHoursSyncServiceTest {
    private val hours = listOf(PlaceBusinessHour(1, LocalTime(9, 0), LocalTime(22, 0)))

    @Test
    fun `미동기화 장소만 조회해 저장한다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1), place(2)))
        val lookup = FakeLookup { hours }
        val service = service(lookup, persistence)

        // 3 은 이미 동기화된 장소라 페이크가 대상에서 제외한다
        val synced = service.sync(listOf(1L, 2L, 3L))

        assertThat(synced).isEqualTo(2)
        assertThat(persistence.saved.keys).containsExactlyInAnyOrder(1L, 2L)
        assertThat(lookup.calls).containsExactlyInAnyOrder("장소1", "장소2")
    }

    @Test
    fun `빈 id 목록은 조회하지 않는다`() {
        val persistence = FakePersistence(unsynced = emptyList())
        val service = service(FakeLookup { hours }, persistence)

        assertThat(service.sync(emptyList())).isEqualTo(0)
        assertThat(persistence.unsyncedCalls).isEqualTo(0)
    }

    @Test
    fun `외부 조회가 실패하면 저장하지 않아 다음 기회에 재시도된다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1)))
        val service = service(FakeLookup { null }, persistence)

        assertThat(service.sync(listOf(1L))).isEqualTo(0)
        assertThat(persistence.saved).isEmpty()
    }

    @Test
    fun `영업시간이 없는 장소는 빈 목록으로 기록해 재조회하지 않는다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1)))
        val service = service(FakeLookup { emptyList() }, persistence)

        assertThat(service.sync(listOf(1L))).isEqualTo(1)
        assertThat(persistence.saved).containsEntry(1L, emptyList<PlaceBusinessHour>())
    }

    @Test
    fun `한 장소의 저장 실패가 나머지 장소 처리를 막지 않는다`() {
        val persistence = FakePersistence(unsynced = listOf(place(1), place(2), place(3)), failOn = setOf(2L))
        val service = service(FakeLookup { hours }, persistence)

        assertThat(service.sync(listOf(1L, 2L, 3L))).isEqualTo(2)
        assertThat(persistence.saved.keys).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    fun `백필은 페이지가 가득 차면 다음 페이지까지 이어서 처리한다`() {
        // 한 페이지(100)가 가득 차면 뒤에 더 있다고 보고 계속 읽는다
        val persistence = FakePersistence(pages = listOf((1L..100L).map { place(it) }, listOf(place(101))))
        val service = service(FakeLookup { hours }, persistence)

        assertThat(service.backfillAll()).isEqualTo(101)
        assertThat(persistence.saved).hasSize(101)
    }

    @Test
    fun `백필은 페이지가 덜 찼으면 더 읽지 않고 멈춘다`() {
        val persistence = FakePersistence(pages = listOf(listOf(place(1), place(2)), listOf(place(3))))
        val service = service(FakeLookup { hours }, persistence)

        // 첫 페이지가 2건(페이지 크기 미만)이라 둘째 페이지는 조회하지 않는다
        assertThat(service.backfillAll()).isEqualTo(2)
        assertThat(persistence.saved.keys).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun `백필 대상이 없으면 아무것도 하지 않는다`() {
        val persistence = FakePersistence(pages = listOf(emptyList()))
        val service = service(FakeLookup { hours }, persistence)

        assertThat(service.backfillAll()).isEqualTo(0)
        assertThat(persistence.saved).isEmpty()
    }

    // 단위 테스트는 실 DB 트랜잭션이 없으므로 콜백만 실행하는 withoutTransaction 을 쓴다.
    private fun service(
        lookup: BusinessHoursLookupPort,
        persistence: PlaceBusinessHourPersistencePort,
    ) = PlaceBusinessHoursSyncService(lookup, persistence, TransactionOperations.withoutTransaction())

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

    private class FakeLookup(
        private val answer: () -> List<PlaceBusinessHour>?,
    ) : BusinessHoursLookupPort {
        val calls = mutableListOf<String>()

        override fun findBusinessHours(
            name: String,
            address: String,
            coordinate: Coordinate,
        ): List<PlaceBusinessHour>? {
            calls += name
            return answer()
        }
    }

    /** unsynced 는 id 조회용 대상, pages 는 백필이 순서대로 받아갈 페이지. failOn 의 장소는 저장에서 예외를 던진다. */
    private class FakePersistence(
        private val unsynced: List<Place> = emptyList(),
        private val pages: List<List<Place>> = emptyList(),
        private val failOn: Set<Long> = emptySet(),
    ) : PlaceBusinessHourPersistencePort {
        val saved = mutableMapOf<Long, List<PlaceBusinessHour>>()
        var unsyncedCalls = 0
        private var page = 0

        override fun findUnsynced(placeIds: List<Long>): List<Place> {
            unsyncedCalls++
            return unsynced.filter { it.id in placeIds }
        }

        override fun findUnsynced(
            afterId: Long?,
            limit: Int,
        ): List<Place> = pages.getOrElse(page++) { emptyList() }

        override fun replaceBusinessHours(
            placeId: Long,
            hours: List<PlaceBusinessHour>,
        ) {
            if (placeId in failOn) error("저장 실패 모사: placeId=$placeId")
            saved[placeId] = hours
        }
    }
}
