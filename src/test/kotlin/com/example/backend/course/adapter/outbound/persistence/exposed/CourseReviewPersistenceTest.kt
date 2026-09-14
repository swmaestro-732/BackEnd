package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.domain.model.CourseReviewStatus
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * course 리뷰 계열 DAO/Table 매핑 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 미배선 [CourseReviewEntity]·[CourseReviewPhotoEntity]·[CourseReviewTagEntity]·[CourseReviewTagLinkTable] 의
 * DAO new/getter/setter 와 Table 컬럼 매핑을 리뷰 저장→조회(사진·태그 포함) 왕복으로 검증한다.
 * course_reviews.created_at/updated_at 은 clientDefault 가 없어 .new 시 명시 지정한다(설정 안 하면 batch insert 실패).
 * FK(course_reviews.course_id→courses)를 만족시키려 실제 course 를 [CourseEntity] 로 만든다.
 * 각 테스트는 transaction { ... rollback() } 로 격리한다.
 */
class CourseReviewPersistenceTest : IntegrationTestBase() {
    @Test
    fun `리뷰를 저장하면 발급된 id 와 설정한 모든 컬럼을 그대로 되읽는다`() {
        transaction {
            val courseId = insertCourse()
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())

            val review =
                CourseReviewEntity.new {
                    this.courseId = courseId
                    status = CourseReviewStatus.PUBLISHED
                    createdAt = now
                    updatedAt = now
                    userId = 909L
                    rating = 5
                    content = "완주 추천 코스"
                }

            assertThat(review.id.value).isPositive()
            assertThat(review.courseId).isEqualTo(courseId)
            assertThat(review.status).isEqualTo(CourseReviewStatus.PUBLISHED)
            assertThat(review.userId).isEqualTo(909L)
            assertThat(review.rating).isEqualTo(5.toShort())
            assertThat(review.content).isEqualTo("완주 추천 코스")
            assertThat(review.createdAt).isEqualTo(now)
            assertThat(review.deletedAt).isNull()

            val row =
                CourseReviewTable
                    .selectAll()
                    .where { CourseReviewTable.id eq review.id }
                    .single()
            assertThat(row[CourseReviewTable.courseId]).isEqualTo(courseId)
            assertThat(row[CourseReviewTable.userId]).isEqualTo(909L)
            assertThat(row[CourseReviewTable.rating]).isEqualTo(5.toShort())
            assertThat(row[CourseReviewTable.content]).isEqualTo("완주 추천 코스")
            assertThat(row[CourseReviewTable.status]).isEqualTo(CourseReviewStatus.PUBLISHED)
            rollback()
        }
    }

    @Test
    fun `content 없는 리뷰와 소프트 삭제 상태 전이를 반영한다`() {
        transaction {
            val courseId = insertCourse()
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            val review =
                CourseReviewEntity.new {
                    this.courseId = courseId
                    status = CourseReviewStatus.PUBLISHED
                    createdAt = now
                    updatedAt = now
                    userId = 3L
                    rating = 2
                    content = null
                }
            assertThat(review.content).isNull()

            val deletedMoment = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            review.status = CourseReviewStatus.DELETED
            review.deletedAt = deletedMoment
            review.updatedAt = deletedMoment

            // 같은 트랜잭션의 DAO 1차 캐시가 아니라 실제 DB 값을 확인한다 — flush 후 Table DSL 로 재조회.
            review.flush()
            val row =
                CourseReviewTable
                    .selectAll()
                    .where { CourseReviewTable.id eq review.id }
                    .single()
            assertThat(row[CourseReviewTable.status]).isEqualTo(CourseReviewStatus.DELETED)
            assertThat(row[CourseReviewTable.deletedAt]).isEqualTo(deletedMoment)
            rollback()
        }
    }

    @Test
    fun `사진은 order_no 순으로 리뷰에 매달려 조회된다`() {
        transaction {
            val review = newReview(insertCourse())

            val first =
                CourseReviewPhotoEntity.new {
                    courseReviewId = review.id.value
                    imageUrl = "https://cdn/a.jpg"
                    orderNo = 0
                }
            CourseReviewPhotoEntity.new {
                courseReviewId = review.id.value
                imageUrl = "https://cdn/b.jpg"
                orderNo = 1
            }

            assertThat(first.imageUrl).isEqualTo("https://cdn/a.jpg")
            assertThat(first.orderNo).isEqualTo(0.toShort())

            val urls =
                CourseReviewPhotoTable
                    .selectAll()
                    .where { CourseReviewPhotoTable.courseReviewId eq review.id.value }
                    .orderBy(CourseReviewPhotoTable.orderNo)
                    .map { it[CourseReviewPhotoTable.imageUrl] }
            assertThat(urls).containsExactly("https://cdn/a.jpg", "https://cdn/b.jpg")
            rollback()
        }
    }

    @Test
    fun `태그를 만들고 링크 테이블로 리뷰에 연결한다`() {
        transaction {
            val review = newReview(insertCourse())
            val worth =
                CourseReviewTagEntity.new {
                    label = "가성비"
                    icon = "coin"
                }
            val view =
                CourseReviewTagEntity.new {
                    label = "뷰맛집"
                    icon = "mountain"
                }

            assertThat(worth.label).isEqualTo("가성비")
            assertThat(worth.icon).isEqualTo("coin")

            listOf(worth, view).forEach { tag ->
                CourseReviewTagLinkTable.insert {
                    it[courseReviewId] = review.id.value
                    it[courseReviewTagId] = tag.id.value
                }
            }

            val linkedTagIds =
                CourseReviewTagLinkTable
                    .selectAll()
                    .where { CourseReviewTagLinkTable.courseReviewId eq review.id.value }
                    .map { it[CourseReviewTagLinkTable.courseReviewTagId] }
            assertThat(linkedTagIds).containsExactlyInAnyOrder(worth.id.value, view.id.value)
            rollback()
        }
    }

    private fun newReview(courseId: Long): CourseReviewEntity {
        val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        return CourseReviewEntity.new {
            this.courseId = courseId
            status = CourseReviewStatus.PUBLISHED
            createdAt = now
            updatedAt = now
            userId = 1L
            rating = 4
            content = null
        }
    }

    /** course_reviews 의 FK(course_id→courses) 를 만족시킬 실제 course 하나. */
    private fun insertCourse(): Long =
        CourseEntity
            .new {
                status = CourseStatus.ACTIVE
                userId = 1L
                title = "리뷰대상 코스"
                coverImageUrl = null
                description = null
                category = null
                area = null
                areaCode = null
                visitDate = null
                isPublished = true
                visibility = CourseVisibility.PUBLIC
                forkedFromId = null
            }.id.value
}
