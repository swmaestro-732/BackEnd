package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewPhotoTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewTagLinkTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.domain.model.CourseReview
import com.example.backend.course.domain.model.CourseReviewTag
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
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
        applyRatingDelta(review.courseId, sumDelta = review.rating.toLong(), cntDelta = 1)

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

    /** courses 별점 카운터(rating_sum·rating_cnt) 상대 갱신 — 리뷰 쓰기와 같은 트랜잭션에서만 부른다. */
    private fun applyRatingDelta(
        courseId: Long,
        sumDelta: Long,
        cntDelta: Int,
    ) {
        CourseTable.update({ CourseTable.id eq courseId }) {
            it[ratingSum] = ratingSum + sumDelta
            it[ratingCnt] = ratingCnt + cntDelta
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

    /** 태그 연결은 마스터 조회 없이 enum 이름을 그대로 심는다(태그 코드가 정본 — V7). */
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
