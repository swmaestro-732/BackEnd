package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewPhotoTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewTagLinkTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.domain.model.CourseReview
import com.example.backend.course.domain.model.CourseReviewStatus
import com.example.backend.course.domain.model.CourseReviewTag
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock

/**
 * course_reviews 와 자식 테이블(course_review_photos·course_review_tag_links) 접근 리포지토리.
 * 전부 DSL 로 쓴다 — 리뷰 본문 한 건은 [insertAndGetId] 로 넣어 id 를 바로 받고,
 * 자식(사진·태그)은 테이블별 [batchInsert] 한 번씩으로 심는다(리뷰 한 건당 statement 3개).
 * created_at 은 테이블 clientDefault 대신 여기서 만든 값을 명시해 넣는다 —
 * 그래야 삽입 후 재조회 없이 반환 객체를 조립할 수 있다([PlaceReviewRepository.insert] 와 같은 방식).
 */
@Repository
class CourseReviewRepository {
    /** 1인 1리뷰 사전검사 — 소프트 삭제된 리뷰는 세지 않아 다시 쓸 수 있다(uq_course_reviews_user_course 와 같은 조건). */
    fun existsActiveReview(
        courseId: Long,
        userId: Long,
    ): Boolean =
        CourseReviewTable
            .selectAll()
            .where {
                (CourseReviewTable.courseId eq courseId) and
                    (CourseReviewTable.userId eq userId) and
                    CourseReviewTable.deletedAt.isNull()
            }.limit(1)
            .empty()
            .not()

    /** 리뷰 본문·사진·태그 연결을 심고, 생성값(id·created_at)까지 채운 도메인 [CourseReview] 로 돌려준다. */
    fun insert(review: CourseReview): CourseReview {
        val now = Clock.System.now()
        val reviewId =
            CourseReviewTable
                .insertAndGetId {
                    it[courseId] = review.courseId
                    it[userId] = review.userId
                    it[status] = review.status
                    it[rating] = review.rating.toShort()
                    it[content] = review.content
                    it[createdAt] = now
                    it[updatedAt] = now
                }.value

        insertPhotos(reviewId, review.photoUrls)
        insertTagLinks(reviewId, review.tags)
        // 카운터는 공개(PUBLISHED) 리뷰만 센다 — 백필(V12)·목록 조회와 같은 기준.
        if (review.status == CourseReviewStatus.PUBLISHED) {
            applyCounterDelta(review.courseId, rating = review.rating, cntDelta = 1, photoDelta = review.photoUrls.size)
        }

        return CourseReview.reconstitute(
            id = reviewId,
            courseId = review.courseId,
            userId = review.userId,
            status = review.status,
            rating = review.rating,
            content = review.content,
            photoUrls = review.photoUrls,
            tags = review.tags,
            createdAt = now,
        )
    }

    fun softDelete(
        reviewId: Long,
        courseId: Long,
        userId: Long,
    ): Int {
        val owned =
            (CourseReviewTable.id eq reviewId) and
                (CourseReviewTable.courseId eq courseId) and
                (CourseReviewTable.userId eq userId) and
                CourseReviewTable.deletedAt.isNull()
        // 삭제 전 상태·별점을 먼저 읽는다 — UPDATE … RETURNING 은 갱신 후 값(DELETED)만 돌려준다.
        val before =
            CourseReviewTable
                .select(CourseReviewTable.rating, CourseReviewTable.status)
                .where { owned }
                .singleOrNull()
                ?: return 0
        val now = Clock.System.now()
        val updated =
            CourseReviewTable.update({ owned }) {
                it[deletedAt] = now
                it[status] = CourseReviewStatus.DELETED
                it[updatedAt] = now
            }
        if (updated == 0) return 0
        // 공개 리뷰였을 때만 카운터를 되돌린다 — 숨김(HIDDEN) 리뷰는 카운터에 들어 있지 않다.
        if (before[CourseReviewTable.status] == CourseReviewStatus.PUBLISHED) {
            val photoCount =
                CourseReviewPhotoTable
                    .selectAll()
                    .where { CourseReviewPhotoTable.courseReviewId eq reviewId }
                    .count()
                    .toInt()
            applyCounterDelta(
                courseId,
                rating = before[CourseReviewTable.rating].toInt(),
                cntDelta = -1,
                photoDelta = -photoCount,
            )
        }
        return 1
    }

    /**
     * courses 카운터 상대 갱신 — 리뷰 쓰기와 같은 트랜잭션에서만 부른다.
     * [cntDelta] 는 +1(작성)/-1(삭제)이고, 별점 합·별점별 수·사진 수를 그 방향으로 함께 움직인다.
     */
    private fun applyCounterDelta(
        courseId: Long,
        rating: Int,
        cntDelta: Int,
        photoDelta: Int,
    ) {
        val ratingColumn = CourseTable.ratingCountColumn(rating)
        CourseTable.update({ CourseTable.id eq courseId }) {
            it[ratingSum] = ratingSum + rating.toLong() * cntDelta
            it[ratingCnt] = ratingCnt + cntDelta
            it[ratingColumn] = ratingColumn + cntDelta
            it[reviewPhotoCnt] = reviewPhotoCnt + photoDelta
        }
    }

    /** 사진은 목록 순서가 곧 노출 순서(order_no)다. */
    private fun insertPhotos(
        reviewId: Long,
        photoUrls: List<String>,
    ) {
        if (photoUrls.isEmpty()) return
        // 생성 id 를 쓰지 않으므로 반환값 조회를 끈다(shouldReturnGeneratedValues = false).
        CourseReviewPhotoTable.batchInsert(photoUrls.withIndex(), shouldReturnGeneratedValues = false) { (index, url) ->
            this[CourseReviewPhotoTable.courseReviewId] = reviewId
            this[CourseReviewPhotoTable.imageUrl] = url
            this[CourseReviewPhotoTable.orderNo] = index.toShort()
        }
    }

    /** 태그 연결은 마스터 조회 없이 enum 이름을 그대로 심는다(태그 코드가 정본 — V6). */
    private fun insertTagLinks(
        reviewId: Long,
        tags: List<CourseReviewTag>,
    ) {
        if (tags.isEmpty()) return
        CourseReviewTagLinkTable.batchInsert(tags, shouldReturnGeneratedValues = false) { tag ->
            this[CourseReviewTagLinkTable.courseReviewId] = reviewId
            this[CourseReviewTagLinkTable.tag] = tag
        }
    }
}
