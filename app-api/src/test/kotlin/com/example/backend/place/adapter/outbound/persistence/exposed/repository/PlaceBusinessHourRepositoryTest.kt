package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceBusinessHourTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceEntity
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.domain.model.PlaceBusinessHour
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import com.example.backend.support.IntegrationTestBase
import kotlinx.datetime.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * [PlaceBusinessHourRepository] 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 행 저장·동기화 시각 기록, 미동기화 대상 선별(동기화 완료·소프트 삭제 제외), 행 잠금(SKIP LOCKED)을 검증한다.
 * 각 테스트는 transaction { ... rollback() } 로 격리한다(잠금 테스트만 다른 커넥션이 봐야 해서 커밋 후 지운다).
 */
class PlaceBusinessHourRepositoryTest
    @Autowired
    constructor(
        private val repository: PlaceBusinessHourRepository,
    ) : IntegrationTestBase() {
        @Test
        fun `saveBusinessHours 는 요일별 행을 넣고 동기화 시각을 기록한다`() {
            transaction {
                val placeId = insertPlace()

                repository.saveBusinessHours(
                    placeId,
                    listOf(
                        PlaceBusinessHour(1, LocalTime(9, 0), LocalTime(22, 0)),
                        PlaceBusinessHour(7, null, null),
                    ),
                )

                assertThat(hoursOf(placeId)).containsOnly(
                    1.toShort() to LocalTime(9, 0),
                    7.toShort() to null,
                )
                assertThat(syncedAtOf(placeId)).isNotNull()
                rollback()
            }
        }

        @Test
        fun `쉬는 시간도 함께 저장되고 되읽힌다`() {
            transaction {
                val placeId = insertPlace()

                repository.saveBusinessHours(
                    placeId,
                    listOf(
                        PlaceBusinessHour(2, LocalTime(11, 0), LocalTime(21, 0), LocalTime(15, 0), LocalTime(17, 0)),
                    ),
                )

                val row =
                    PlaceBusinessHourTable
                        .selectAll()
                        .where { PlaceBusinessHourTable.placeId eq placeId }
                        .single()
                assertThat(row[PlaceBusinessHourTable.breakStart]).isEqualTo(LocalTime(15, 0))
                assertThat(row[PlaceBusinessHourTable.breakEnd]).isEqualTo(LocalTime(17, 0))
                rollback()
            }
        }

        @Test
        fun `영업시간이 빈 목록이면 행 없이 동기화 시각만 남는다`() {
            transaction {
                val placeId = insertPlace()

                repository.saveBusinessHours(placeId, emptyList())

                assertThat(hoursOf(placeId)).isEmpty()
                assertThat(syncedAtOf(placeId)).isNotNull()
                rollback()
            }
        }

        @Test
        fun `findByPlaceId 는 저장한 행을 요일 순 도메인 모델로 되돌려 준다`() {
            transaction {
                val placeId = insertPlace()
                val saturday = PlaceBusinessHour(6, LocalTime(10, 0), LocalTime(23, 0))
                val monday = PlaceBusinessHour(1, LocalTime(9, 0), LocalTime(22, 0), LocalTime(15, 0), LocalTime(17, 0))
                val sunday = PlaceBusinessHour(7, null, null)
                repository.saveBusinessHours(placeId, listOf(saturday, monday, sunday))

                val found = repository.findByPlaceId(placeId).map { it.toDomain() }

                assertThat(found).containsExactly(monday, saturday, sunday)
                assertThat(repository.findByPlaceId(insertPlace())).isEmpty()
                rollback()
            }
        }

        @Test
        fun `findUnsynced 는 동기화된 장소와 삭제된 장소를 뺀다`() {
            transaction {
                val pending = insertPlace()
                val done = insertPlace()
                val deleted = insertPlace(deleted = true)
                repository.saveBusinessHours(done, emptyList())

                val byIds = repository.findUnsynced(listOf(pending, done, deleted)).map { it.id.value }

                assertThat(byIds).containsExactly(pending)
                rollback()
            }
        }

        @Test
        fun `findUnsynced 는 빈 id 목록에 빈 결과를 준다`() {
            transaction {
                assertThat(repository.findUnsynced(emptyList())).isEmpty()
                rollback()
            }
        }

        @Test
        fun `lockUnsynced 는 다른 트랜잭션이 잠근 장소를 기다리지 않고 건너뛴다`() {
            // 다른 커넥션이 보려면 커밋돼 있어야 한다
            val placeId = transaction { insertPlace() }
            val other = Executors.newSingleThreadExecutor()
            try {
                transaction {
                    assertThat(repository.lockUnsynced(placeId)?.id?.value).isEqualTo(placeId)

                    // Long? 로 받아야 AssertJ 가 원시 long 오버로드로 언박싱하지 않는다
                    val seenByOther: Long? =
                        other
                            .submit<Long?> { transaction { repository.lockUnsynced(placeId)?.id?.value } }
                            .get(10, TimeUnit.SECONDS)

                    assertThat(seenByOther).isNull()
                    rollback()
                }
            } finally {
                other.shutdownNow()
                transaction { PlaceTable.deleteWhere { PlaceTable.id eq placeId } }
            }
        }

        private fun hoursOf(placeId: Long) =
            PlaceBusinessHourTable
                .selectAll()
                .where { PlaceBusinessHourTable.placeId eq placeId }
                .map { it[PlaceBusinessHourTable.dayOfWeek] to it[PlaceBusinessHourTable.openTime] }

        private fun syncedAtOf(placeId: Long) =
            PlaceTable
                .selectAll()
                .where { PlaceTable.id eq placeId }
                .single()[PlaceTable.businessHoursSyncedAt]

        /** place_business_hours 의 FK(place_id→places) 를 만족시킬 실제 place 하나. */
        private fun insertPlace(deleted: Boolean = false): Long =
            PlaceEntity
                .new {
                    status = PlaceStatus.ACTIVE
                    name = "영업시간 동기화 대상"
                    description = null
                    category = PlaceCategory.CAFE
                    location = Coordinate(latitude = 37.5, longitude = 127.0)
                    address = "서울시 어딘가 1"
                    areaCode = null
                    imageUrl = null
                    businessStatus = PlaceBusinessStatus.OPEN
                    kakaoPlaceId = null
                    if (deleted) deletedAt = Clock.System.now()
                }.id.value
    }
