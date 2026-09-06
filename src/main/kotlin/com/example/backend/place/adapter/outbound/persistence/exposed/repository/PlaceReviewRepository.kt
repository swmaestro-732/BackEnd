package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceReviewPhotoTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceReviewTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceReviewTagLinkTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.domain.model.PlaceReview
import com.example.backend.place.domain.model.PlaceReviewStatus
import com.example.backend.place.domain.model.PlaceReviewTag
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import org.springframework.stereotype.Repository
import kotlin.time.Clock

/**
 * place_reviews 와 자식 테이블(place_review_photos·place_review_tag_links) 접근 리포지토리.
 * 전부 DSL 로 쓴다 — 리뷰 본문 한 건은 [insertAndGetId] 로 넣어 id 를 바로 받고,
 * 자식(사진·태그)은 테이블별 [batchInsert] 한 번씩으로 심는다(리뷰 한 건당 statement 3개).
 * created_at 은 테이블 clientDefault 대신 여기서 만든 값을 명시해 넣는다 —
 * 그래야 삽입 후 재조회 없이 반환 객체를 조립할 수 있다([SavedPlaceRepository.insert] 와 같은 방식).
 */
@Repository
class PlaceReviewRepository {
    /** 리뷰 본문·사진·태그 연결을 심고, 생성값(id·created_at)까지 채운 도메인 [PlaceReview] 로 돌려준다. */
    fun insert(review: PlaceReview): PlaceReview {
        val now = Clock.System.now()
        val reviewId =
            PlaceReviewTable
                .insertAndGetId {
                    it[placeId] = review.placeId
                    it[userId] = review.userId
                    it[status] = review.status
                    it[rating] = review.rating.toShort()
                    it[content] = review.content
                    it[createdAt] = now
                    it[updatedAt] = now
                }.value

        insertPhotos(reviewId, review.photoUrls)
        insertTagLinks(reviewId, review.tags)
        applyRatingDelta(review.placeId, sumDelta = review.rating.toLong(), cntDelta = 1)

        return PlaceReview.reconstitute(
            id = reviewId,
            placeId = review.placeId,
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
        placeId: Long,
        userId: Long,
    ): Int {
        val now = Clock.System.now()
        val deletedRating =
            PlaceReviewTable
                .updateReturning(
                    returning = listOf(PlaceReviewTable.rating),
                    where = {
                        (PlaceReviewTable.id eq reviewId) and
                            (PlaceReviewTable.placeId eq placeId) and
                            (PlaceReviewTable.userId eq userId) and
                            PlaceReviewTable.deletedAt.isNull()
                    },
                ) {
                    it[deletedAt] = now
                    it[status] = PlaceReviewStatus.DELETED
                    it[updatedAt] = now
                }.singleOrNull()
                ?.get(PlaceReviewTable.rating)
                ?: return 0
        applyRatingDelta(placeId, sumDelta = -deletedRating.toLong(), cntDelta = -1)
        return 1
    }

    /** places 별점 카운터(rating_sum·rating_cnt) 상대 갱신 — 리뷰 쓰기와 같은 트랜잭션에서만 부른다. */
    private fun applyRatingDelta(
        placeId: Long,
        sumDelta: Long,
        cntDelta: Int,
    ) {
        PlaceTable.update({ PlaceTable.id eq placeId }) {
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
        PlaceReviewPhotoTable.batchInsert(photoUrls.withIndex(), shouldReturnGeneratedValues = false) { (index, url) ->
            this[PlaceReviewPhotoTable.placeReviewId] = reviewId
            this[PlaceReviewPhotoTable.imageUrl] = url
            this[PlaceReviewPhotoTable.orderNo] = index.toShort()
        }
    }

    /** 태그 연결은 마스터 조회 없이 enum 이름을 그대로 심는다(태그 코드가 정본 — V4). */
    private fun insertTagLinks(
        reviewId: Long,
        tags: List<PlaceReviewTag>,
    ) {
        if (tags.isEmpty()) return
        PlaceReviewTagLinkTable.batchInsert(tags, shouldReturnGeneratedValues = false) { tag ->
            this[PlaceReviewTagLinkTable.placeReviewId] = reviewId
            this[PlaceReviewTagLinkTable.tag] = tag
        }
    }
}
