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
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.Clock

/**
 * [PlaceBusinessHourRepository] 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 행 교체·동기화 시각 기록과 미동기화 대상 선별(동기화 완료·소프트 삭제 제외)을 검증한다.
 * 각 테스트는 transaction { ... rollback() } 로 격리한다.
 */
class PlaceBusinessHourRepositoryTest
    @Autowired
    constructor(
        private val repository: PlaceBusinessHourRepository,
    ) : IntegrationTestBase() {
        @Test
        fun `replaceBusinessHours 는 요일별 행을 넣고 동기화 시각을 기록한다`() {
            transaction {
                val placeId = insertPlace()

                repository.replaceBusinessHours(
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
        fun `replaceBusinessHours 를 다시 부르면 기존 행을 지우고 새로 넣는다`() {
            transaction {
                val placeId = insertPlace()
                repository.replaceBusinessHours(
                    placeId,
                    listOf(PlaceBusinessHour(1, LocalTime(9, 0), LocalTime(22, 0))),
                )

                repository.replaceBusinessHours(
                    placeId,
                    listOf(PlaceBusinessHour(2, LocalTime(11, 0), LocalTime(20, 0))),
                )

                // 유니크 충돌 없이 교체된다 — 월요일 행은 사라지고 화요일 행만 남는다
                assertThat(hoursOf(placeId)).containsOnly(2.toShort() to LocalTime(11, 0))
                rollback()
            }
        }

        @Test
        fun `영업시간이 빈 목록이면 행 없이 동기화 시각만 남는다`() {
            transaction {
                val placeId = insertPlace()

                repository.replaceBusinessHours(placeId, emptyList())

                assertThat(hoursOf(placeId)).isEmpty()
                assertThat(syncedAtOf(placeId)).isNotNull()
                rollback()
            }
        }

        @Test
        fun `findUnsynced 는 동기화된 장소와 삭제된 장소를 뺀다`() {
            transaction {
                val pending = insertPlace()
                val done = insertPlace()
                val deleted = insertPlace(deleted = true)
                repository.replaceBusinessHours(done, emptyList())

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
        fun `findUnsynced 페이징은 afterId 다음부터 id 오름차순으로 준다`() {
            transaction {
                val first = insertPlace()
                val second = insertPlace()

                val page = repository.findUnsynced(afterId = first - 1, limit = 2).map { it.id.value }

                assertThat(page).startsWith(first, second)
                assertThat(repository.findUnsynced(afterId = second, limit = 2).map { it.id.value })
                    .doesNotContain(first, second)
                rollback()
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
