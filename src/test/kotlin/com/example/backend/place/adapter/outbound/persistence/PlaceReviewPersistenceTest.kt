package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.common.persistence.postgis.GeoPoint
import com.example.backend.place.application.port.outbound.PlaceReviewPersistencePort
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceReview
import com.example.backend.place.domain.model.PlaceReviewStatus
import com.example.backend.place.domain.model.PlaceReviewTag
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit
import kotlin.time.toJavaInstant

/**
 * place_reviews 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * DSL insert 가 생성 id·작성 시각을 재조회 없이 돌려주는지, 자식(사진 순서·태그 코드)이 함께 심기는지,
 * 태그가 마스터 테이블 없이 enum 이름으로 저장되는지(V4) 검증한다.
 * 각 테스트는 transaction { ... rollback() } 으로 격리한다(픽스처 오염 없음).
 */
class PlaceReviewPersistenceTest
    @Autowired
    constructor(
        private val port: PlaceReviewPersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `insert 는 생성된 id 와 작성 시각을 담은 도메인을 반환하고 DB 행과 일치한다`() {
            transaction {
                val placeId = insertPlace("어니언 성수")

                val saved = port.save(review(placeId, rating = 5, content = "통창 뷰가 좋아요"))

                assertTrue(requireNotNull(saved.id) > 0) // 시퀀스가 발급한 id 를 되받았다
                assertEquals(placeId, saved.placeId)
                assertEquals(USER_ID, saved.userId)

                val row = PlaceReviewTable.selectAll().where { PlaceReviewTable.id eq saved.id!! }.single()
                assertEquals(placeId, row[PlaceReviewTable.placeId])
                assertEquals(USER_ID, row[PlaceReviewTable.userId])
                assertEquals(PlaceReviewStatus.PUBLISHED, row[PlaceReviewTable.status])
                assertEquals(5.toShort(), row[PlaceReviewTable.rating])
                assertEquals("통창 뷰가 좋아요", row[PlaceReviewTable.content])
                assertNull(row[PlaceReviewTable.deletedAt])

                // 반환한 createdAt 이 실제 저장된 created_at 과 같은 시각인지.
                // ms 로 절삭해 비교한다 — Postgres timestamptz 는 µs 까지만 담아 더 미세한 자리는 플랫폼에 의존한다.
                assertEquals(
                    row[PlaceReviewTable.createdAt].toJavaInstant().truncatedTo(ChronoUnit.MILLIS),
                    requireNotNull(saved.createdAt).toJavaInstant().truncatedTo(ChronoUnit.MILLIS),
                )
                rollback()
            }
        }

        @Test
        fun `사진은 목록 순서대로 order_no 를 받아 저장된다`() {
            transaction {
                val placeId = insertPlace("센터커피")
                val photoUrls =
                    listOf(
                        "https://cdn.example.com/1.jpg",
                        "https://cdn.example.com/2.jpg",
                        "https://cdn.example.com/3.jpg",
                    )

                val saved = port.save(review(placeId, photoUrls = photoUrls))

                val rows =
                    PlaceReviewPhotoTable
                        .selectAll()
                        .where { PlaceReviewPhotoTable.placeReviewId eq saved.id!! }
                        .orderBy(PlaceReviewPhotoTable.orderNo to SortOrder.ASC)
                        .map { it[PlaceReviewPhotoTable.imageUrl] to it[PlaceReviewPhotoTable.orderNo] }

                assertEquals(photoUrls.mapIndexed { index, url -> url to index.toShort() }, rows)
                rollback()
            }
        }

        @Test
        fun `태그는 마스터 조회 없이 enum 이름으로 저장된다`() {
            transaction {
                val placeId = insertPlace("리움미술관")

                val saved = port.save(review(placeId, tags = listOf(PlaceReviewTag.COFFEE, PlaceReviewTag.VIEW)))

                val tags =
                    PlaceReviewTagLinkTable
                        .selectAll()
                        .where { PlaceReviewTagLinkTable.placeReviewId eq saved.id!! }
                        .map { it[PlaceReviewTagLinkTable.tag] }

                assertEquals(setOf(PlaceReviewTag.COFFEE, PlaceReviewTag.VIEW), tags.toSet())
                rollback()
            }
        }

        @Test
        fun `사진·태그가 없으면 자식 행도 없다`() {
            transaction {
                val placeId = insertPlace("빈 리뷰용 장소")

                val saved = port.save(review(placeId))

                assertTrue(
                    PlaceReviewPhotoTable
                        .selectAll()
                        .where { PlaceReviewPhotoTable.placeReviewId eq saved.id!! }
                        .empty(),
                )
                assertTrue(
                    PlaceReviewTagLinkTable
                        .selectAll()
                        .where { PlaceReviewTagLinkTable.placeReviewId eq saved.id!! }
                        .empty(),
                )
                rollback()
            }
        }

        @Test
        fun `같은 사용자가 같은 장소에 여러 번 남길 수 있다`() {
            transaction {
                val placeId = insertPlace("재방문 장소")

                val first = port.save(review(placeId, rating = 5))
                val second = port.save(review(placeId, rating = 3))

                assertNotEquals(first.id, second.id)
                assertEquals(
                    2,
                    PlaceReviewTable
                        .selectAll()
                        .where { PlaceReviewTable.placeId eq placeId }
                        .count()
                        .toInt(),
                )
                rollback()
            }
        }

        private fun review(
            placeId: Long,
            rating: Int = 4,
            content: String? = null,
            photoUrls: List<String> = emptyList(),
            tags: List<PlaceReviewTag> = emptyList(),
        ) = PlaceReview.create(
            placeId = placeId,
            userId = USER_ID,
            rating = rating,
            content = content,
            photoUrls = photoUrls,
            tags = tags,
        )

        /** place_reviews.place_id 에는 FK 가 있어 리뷰마다 실제 장소 행이 필요하다. */
        private fun insertPlace(name: String): Long =
            PlaceTable
                .insertAndGetId {
                    it[PlaceTable.name] = name
                    it[category] = PlaceCategory.CAFE
                    it[location] = GeoPoint(latitude = 37.5446, longitude = 127.0559)
                    it[address] = "서울 성동구 아차산로 100"
                }.value

        private companion object {
            const val USER_ID = 1L
        }
    }
