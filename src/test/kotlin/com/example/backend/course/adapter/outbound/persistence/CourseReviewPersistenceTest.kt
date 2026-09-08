package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewPhotoTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewTagLinkTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.application.port.outbound.CourseReviewPersistencePort
import com.example.backend.course.domain.model.CourseReview
import com.example.backend.course.domain.model.CourseReviewStatus
import com.example.backend.course.domain.model.CourseReviewTag
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit
import kotlin.time.toJavaInstant

/**
 * course_reviews 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * DSL insert 가 생성 id·작성 시각을 재조회 없이 돌려주는지, 자식(사진 순서·태그 코드)이 함께 심기는지,
 * 태그가 마스터 테이블 없이 enum 이름으로 저장되는지(V5), 별점 카운터(courses.rating_sum/rating_cnt)가
 * 작성·소프트 삭제와 같은 트랜잭션에서 상대 갱신되는지 검증한다.
 * 각 테스트는 transaction { ... rollback() } 으로 격리한다(픽스처 오염 없음).
 */
class CourseReviewPersistenceTest
    @Autowired
    constructor(
        private val port: CourseReviewPersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `insert 는 생성된 id 와 작성 시각을 담은 도메인을 반환하고 DB 행과 일치한다`() {
            transaction {
                val courseId = insertCourse("성수 카페 코스")

                val saved = port.save(review(courseId, rating = 5, content = "동선이 자연스러워요"))

                assertTrue(requireNotNull(saved.id) > 0) // 시퀀스가 발급한 id 를 되받았다
                assertEquals(courseId, saved.courseId)
                assertEquals(USER_ID, saved.userId)

                val row = CourseReviewTable.selectAll().where { CourseReviewTable.id eq saved.id!! }.single()
                assertEquals(courseId, row[CourseReviewTable.courseId])
                assertEquals(USER_ID, row[CourseReviewTable.userId])
                assertEquals(CourseReviewStatus.PUBLISHED, row[CourseReviewTable.status])
                assertEquals(5.toShort(), row[CourseReviewTable.rating])
                assertEquals("동선이 자연스러워요", row[CourseReviewTable.content])
                assertNull(row[CourseReviewTable.deletedAt])

                // 반환한 createdAt 이 실제 저장된 created_at 과 같은 시각인지.
                // ms 로 절삭해 비교한다 — Postgres timestamptz 는 µs 까지만 담아 더 미세한 자리는 플랫폼에 의존한다.
                assertEquals(
                    row[CourseReviewTable.createdAt].toJavaInstant().truncatedTo(ChronoUnit.MILLIS),
                    requireNotNull(saved.createdAt).toJavaInstant().truncatedTo(ChronoUnit.MILLIS),
                )
                rollback()
            }
        }

        @Test
        fun `사진은 목록 순서대로 order_no 를 받아 저장된다`() {
            transaction {
                val courseId = insertCourse("사진 코스")
                val photoUrls =
                    listOf(
                        "https://cdn.example.com/1.jpg",
                        "https://cdn.example.com/2.jpg",
                        "https://cdn.example.com/3.jpg",
                    )

                val saved = port.save(review(courseId, photoUrls = photoUrls))

                val rows =
                    CourseReviewPhotoTable
                        .selectAll()
                        .where { CourseReviewPhotoTable.courseReviewId eq saved.id!! }
                        .orderBy(CourseReviewPhotoTable.orderNo to SortOrder.ASC)
                        .map { it[CourseReviewPhotoTable.imageUrl] to it[CourseReviewPhotoTable.orderNo] }

                assertEquals(photoUrls.mapIndexed { index, url -> url to index.toShort() }, rows)
                rollback()
            }
        }

        @Test
        fun `태그는 마스터 조회 없이 enum 이름으로 저장된다`() {
            transaction {
                val courseId = insertCourse("태그 코스")

                val saved = port.save(review(courseId, tags = setOf(CourseReviewTag.PACKED, CourseReviewTag.SMOOTH)))

                val tags =
                    CourseReviewTagLinkTable
                        .selectAll()
                        .where { CourseReviewTagLinkTable.courseReviewId eq saved.id!! }
                        .map { it[CourseReviewTagLinkTable.tag] }

                assertEquals(setOf(CourseReviewTag.PACKED, CourseReviewTag.SMOOTH), tags.toSet())
                rollback()
            }
        }

        @Test
        fun `사진·태그가 없으면 자식 행도 없다`() {
            transaction {
                val courseId = insertCourse("빈 리뷰용 코스")

                val saved = port.save(review(courseId))

                assertTrue(
                    CourseReviewPhotoTable
                        .selectAll()
                        .where { CourseReviewPhotoTable.courseReviewId eq saved.id!! }
                        .empty(),
                )
                assertTrue(
                    CourseReviewTagLinkTable
                        .selectAll()
                        .where { CourseReviewTagLinkTable.courseReviewId eq saved.id!! }
                        .empty(),
                )
                rollback()
            }
        }

        @Test
        fun `작성마다 별점 카운터가 누적되고 같은 사용자의 중복 작성도 막지 않는다`() {
            transaction {
                val courseId = insertCourse("재따라가기 코스")

                val first = port.save(review(courseId, rating = 5))
                val second = port.save(review(courseId, rating = 3))

                assertNotEquals(first.id, second.id)
                assertEquals(8L to 2, ratingCounters(courseId)) // +5/+1, +3/+1
                rollback()
            }
        }

        @Test
        fun `소프트 삭제는 상태·deleted_at 을 스탬프하고 카운터를 되돌린다`() {
            transaction {
                val courseId = insertCourse("삭제 코스")
                val saved = port.save(review(courseId, rating = 4))

                val deleted = port.softDelete(reviewId = saved.id!!, courseId = courseId, userId = USER_ID)

                assertEquals(1, deleted)
                val row = CourseReviewTable.selectAll().where { CourseReviewTable.id eq saved.id!! }.single()
                assertEquals(CourseReviewStatus.DELETED, row[CourseReviewTable.status])
                assertNotNull(row[CourseReviewTable.deletedAt])
                assertEquals(0L to 0, ratingCounters(courseId))
                rollback()
            }
        }

        @Test
        fun `타인 리뷰·다른 코스·이미 삭제된 리뷰는 0을 돌려주고 카운터를 건드리지 않는다`() {
            transaction {
                val courseId = insertCourse("은닉 코스")
                val saved = port.save(review(courseId, rating = 4))

                // 타인 리뷰 — WHERE 절의 소유권 조건에 걸린다.
                assertEquals(0, port.softDelete(reviewId = saved.id!!, courseId = courseId, userId = USER_ID + 1))
                // 다른 코스 경로로 지우려는 시도 — 소속 조건에 걸린다.
                assertEquals(0, port.softDelete(reviewId = saved.id!!, courseId = courseId + 1, userId = USER_ID))
                assertEquals(4L to 1, ratingCounters(courseId))

                // 정상 삭제 후 재삭제 — deleted_at IS NULL 조건에 걸려 카운터가 두 번 줄지 않는다.
                assertEquals(1, port.softDelete(reviewId = saved.id!!, courseId = courseId, userId = USER_ID))
                assertEquals(0, port.softDelete(reviewId = saved.id!!, courseId = courseId, userId = USER_ID))
                assertEquals(0L to 0, ratingCounters(courseId))
                rollback()
            }
        }

        private fun review(
            courseId: Long,
            rating: Int = 4,
            content: String? = null,
            photoUrls: List<String> = emptyList(),
            tags: Set<CourseReviewTag> = emptySet(),
        ) = CourseReview.create(
            courseId = courseId,
            userId = USER_ID,
            rating = rating,
            content = content,
            photoUrls = photoUrls,
            tags = tags,
        )

        /** course_reviews.course_id 에는 FK 가 있어 리뷰마다 실제 코스 행이 필요하다. */
        private fun insertCourse(title: String): Long =
            CourseTable
                .insertAndGetId {
                    it[status] = CourseStatus.ACTIVE
                    it[userId] = USER_ID
                    it[CourseTable.title] = title
                    it[isPublished] = true
                    it[visibility] = CourseVisibility.PUBLIC
                }.value

        private fun ratingCounters(courseId: Long): Pair<Long, Int> {
            val row = CourseTable.selectAll().where { CourseTable.id eq courseId }.single()
            return row[CourseTable.ratingSum] to row[CourseTable.ratingCnt]
        }

        private companion object {
            const val USER_ID = 1L
        }
    }
