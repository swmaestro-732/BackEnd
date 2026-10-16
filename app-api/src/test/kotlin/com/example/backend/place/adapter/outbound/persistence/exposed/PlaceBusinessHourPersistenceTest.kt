package com.example.backend.place.adapter.outbound.persistence.exposed

import com.example.backend.common.geo.Coordinate
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

/**
 * place_business_hours DAO/Table 매핑 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 아직 프로덕션 어댑터가 배선되지 않은 [PlaceBusinessHourEntity] 의 DAO new/getter 와 Table 컬럼 매핑을
 * 요일별 저장→조회 왕복으로 검증한다(휴무는 open/close NULL). FK(place_business_hours.place_id→places)를
 * 만족시키기 위해 실제 place 를 [PlaceEntity] 로 만든다. 각 테스트는 transaction { ... rollback() } 로 격리한다.
 */
class PlaceBusinessHourPersistenceTest : IntegrationTestBase() {
    @Test
    fun `영업시간은 place 별 요일로 저장되고 널 가능한 시간을 담는다`() {
        transaction {
            val placeId = insertPlace()

            val monday =
                PlaceBusinessHourEntity.new {
                    this.placeId = placeId
                    dayOfWeek = 1
                    openTime = LocalTime(9, 0)
                    closeTime = LocalTime(22, 0)
                }
            val sunday =
                PlaceBusinessHourEntity.new {
                    this.placeId = placeId
                    dayOfWeek = 7
                    openTime = null // 휴무
                    closeTime = null
                }

            assertThat(monday.dayOfWeek).isEqualTo(1.toShort())
            assertThat(monday.openTime).isEqualTo(LocalTime(9, 0))
            assertThat(monday.closeTime).isEqualTo(LocalTime(22, 0))
            assertThat(sunday.openTime).isNull()

            val rows =
                PlaceBusinessHourTable
                    .selectAll()
                    .where { PlaceBusinessHourTable.placeId eq placeId }
                    .associate { it[PlaceBusinessHourTable.dayOfWeek] to it[PlaceBusinessHourTable.openTime] }
            assertThat(rows).containsEntry(1.toShort(), LocalTime(9, 0))
            assertThat(rows).containsEntry(7.toShort(), null)
            rollback()
        }
    }

    @Test
    fun `같은 place 같은 요일은 유니크 제약이 막는다`() {
        transaction {
            val placeId = insertPlace()
            PlaceBusinessHourEntity.new {
                this.placeId = placeId
                dayOfWeek = 2
                openTime = LocalTime(10, 0)
                closeTime = LocalTime(20, 0)
            }
            org.junit.jupiter.api.assertThrows<Exception> {
                PlaceBusinessHourEntity.new {
                    this.placeId = placeId
                    dayOfWeek = 2 // uq_place_business_hours_place_day 위반
                    openTime = LocalTime(11, 0)
                    closeTime = LocalTime(21, 0)
                }
                // DAO flush 를 강제해 제약 위반을 트랜잭션 밖으로 끌어낸다
                PlaceBusinessHourTable.selectAll().where { PlaceBusinessHourTable.placeId eq placeId }.toList()
            }
            rollback()
        }
    }

    /** place_business_hours 의 FK(place_id→places) 를 만족시킬 실제 place 하나. */
    private fun insertPlace(): Long =
        PlaceEntity
            .new {
                status = PlaceStatus.ACTIVE
                name = "영업시간 대상 카페"
                description = null
                category = PlaceCategory.CAFE
                location = Coordinate(latitude = 37.5, longitude = 127.0)
                address = "서울시 어딘가 1"
                areaCode = null
                imageUrl = null
                businessStatus = PlaceBusinessStatus.OPEN
                kakaoPlaceId = null
            }.id.value
}
