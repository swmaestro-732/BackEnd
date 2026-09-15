package com.example.backend.place.adapter.outbound.persistence.exposed

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceReviewStatus
import com.example.backend.place.domain.model.PlaceStatus
import com.example.backend.support.IntegrationTestBase
import kotlinx.datetime.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * place 리뷰 계열 DAO/Table 매핑 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 아직 프로덕션 어댑터가 배선되지 않은 [PlaceReviewEntity]·[PlaceReviewPhotoEntity]·[PlaceReviewTagEntity]·
 * [PlaceReviewTagLinkTable]·[PlaceBusinessHourEntity] 의 DAO new/getter/setter 와 Table 컬럼 매핑을,
 * 리뷰 저장→조회(사진·태그·영업시간 포함) 왕복으로 검증한다. FK(place_reviews.place_id→places 등)를
 * 만족시키기 위해 실제 place 를 [PlaceEntity] 로 만든다. 각 테스트는 transaction { ... rollback() } 로 격리한다.
 */
class PlaceReviewPersistenceTest : IntegrationTestBase() {
    @Test
    fun `리뷰를 저장하면 발급된 id 와 설정한 모든 컬럼을 그대로 되읽는다`() {
        transaction {
            val placeId = insertPlace()

            val review =
                PlaceReviewEntity.new {
                    this.placeId = placeId
                    status = PlaceReviewStatus.PUBLISHED
                    userId = 4242L
                    rating = 5
                    content = "인생 카페"
                }

            assertThat(review.id.value).isPositive()
            // DAO getter 왕복
            assertThat(review.placeId).isEqualTo(placeId)
            assertThat(review.status).isEqualTo(PlaceReviewStatus.PUBLISHED)
            assertThat(review.userId).isEqualTo(4242L)
            assertThat(review.rating).isEqualTo(5.toShort())
            assertThat(review.content).isEqualTo("인생 카페")
            assertThat(review.deletedAt).isNull()
            assertThat(review.createdAt).isNotNull() // clientDefault 가 채운다
            assertThat(review.updatedAt).isNotNull()

            // DSL 로도 같은 행이 보인다(DAO·DSL 공용 매핑)
            val row =
                PlaceReviewTable
                    .selectAll()
                    .where { PlaceReviewTable.id eq review.id }
                    .single()
            assertThat(row[PlaceReviewTable.placeId]).isEqualTo(placeId)
            assertThat(row[PlaceReviewTable.userId]).isEqualTo(4242L)
            assertThat(row[PlaceReviewTable.rating]).isEqualTo(5.toShort())
            assertThat(row[PlaceReviewTable.content]).isEqualTo("인생 카페")
            assertThat(row[PlaceReviewTable.status]).isEqualTo(PlaceReviewStatus.PUBLISHED)
            rollback()
        }
    }

    @Test
    fun `content 없는 리뷰와 소프트 삭제 상태 전이를 반영한다`() {
        transaction {
            val placeId = insertPlace()

            val review =
                PlaceReviewEntity.new {
                    this.placeId = placeId
                    status = PlaceReviewStatus.PUBLISHED
                    userId = 7L
                    rating = 3
                    content = null
                }
            assertThat(review.content).isNull()

            val deletedMoment = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            review.status = PlaceReviewStatus.DELETED
            review.deletedAt = deletedMoment
            review.updatedAt = deletedMoment

            // 같은 트랜잭션의 DAO 1차 캐시가 아니라 실제 DB 값을 확인한다 — flush 후 Table DSL 로 재조회.
            review.flush()
            val row =
                PlaceReviewTable
                    .selectAll()
                    .where { PlaceReviewTable.id eq review.id }
                    .single()
            assertThat(row[PlaceReviewTable.status]).isEqualTo(PlaceReviewStatus.DELETED)
            assertThat(row[PlaceReviewTable.deletedAt]).isEqualTo(deletedMoment)
            rollback()
        }
    }

    @Test
    fun `사진은 order_no 순으로 리뷰에 매달려 조회된다`() {
        transaction {
            val placeId = insertPlace()
            val review =
                PlaceReviewEntity.new {
                    this.placeId = placeId
                    status = PlaceReviewStatus.PUBLISHED
                    userId = 1L
                    rating = 4
                    content = "사진 많음"
                }

            val first =
                PlaceReviewPhotoEntity.new {
                    placeReviewId = review.id.value
                    imageUrl = "https://cdn/1.jpg"
                    orderNo = 0
                }
            PlaceReviewPhotoEntity.new {
                placeReviewId = review.id.value
                imageUrl = "https://cdn/2.jpg"
                orderNo = 1
            }

            assertThat(first.imageUrl).isEqualTo("https://cdn/1.jpg")
            assertThat(first.orderNo).isEqualTo(0.toShort())

            val urls =
                PlaceReviewPhotoTable
                    .selectAll()
                    .where { PlaceReviewPhotoTable.placeReviewId eq review.id.value }
                    .orderBy(PlaceReviewPhotoTable.orderNo)
                    .map { it[PlaceReviewPhotoTable.imageUrl] }
            assertThat(urls).containsExactly("https://cdn/1.jpg", "https://cdn/2.jpg")
            rollback()
        }
    }

    @Test
    fun `태그를 만들고 링크 테이블로 리뷰에 연결한다`() {
        transaction {
            val placeId = insertPlace()
            val review =
                PlaceReviewEntity.new {
                    this.placeId = placeId
                    status = PlaceReviewStatus.PUBLISHED
                    userId = 1L
                    rating = 5
                    content = null
                }
            val mood =
                PlaceReviewTagEntity.new {
                    label = "분위기좋아요"
                    icon = "sparkles"
                }
            val clean =
                PlaceReviewTagEntity.new {
                    label = "청결해요"
                    icon = "broom"
                }

            assertThat(mood.label).isEqualTo("분위기좋아요")
            assertThat(mood.icon).isEqualTo("sparkles")

            listOf(mood, clean).forEach { tag ->
                PlaceReviewTagLinkTable.insert {
                    it[placeReviewId] = review.id.value
                    it[placeReviewTagId] = tag.id.value
                }
            }

            val linkedTagIds =
                PlaceReviewTagLinkTable
                    .selectAll()
                    .where { PlaceReviewTagLinkTable.placeReviewId eq review.id.value }
                    .map { it[PlaceReviewTagLinkTable.placeReviewTagId] }
            assertThat(linkedTagIds).containsExactlyInAnyOrder(mood.id.value, clean.id.value)
            rollback()
        }
    }

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

    /** place_reviews·place_business_hours 의 FK(place_id→places) 를 만족시킬 실제 place 하나. */
    private fun insertPlace(): Long =
        PlaceEntity
            .new {
                status = PlaceStatus.ACTIVE
                name = "리뷰대상 카페"
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
