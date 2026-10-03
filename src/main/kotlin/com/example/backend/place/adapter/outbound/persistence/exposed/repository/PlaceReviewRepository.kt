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
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
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
    /** 1인 1리뷰 사전검사 — 소프트 삭제된 리뷰는 세지 않아 다시 쓸 수 있다(uq_place_reviews_user_place 와 같은 조건). */
    fun existsActiveReview(
        placeId: Long,
        userId: Long,
    ): Boolean =
        PlaceReviewTable
            .selectAll()
            .where {
                (PlaceReviewTable.placeId eq placeId) and
                    (PlaceReviewTable.userId eq userId) and
                    PlaceReviewTable.deletedAt.isNull()
            }.limit(1)
            .empty()
            .not()

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
        // 카운터는 공개(PUBLISHED) 리뷰만 센다 — 백필(V12)·목록 조회와 같은 기준.
        if (review.status == PlaceReviewStatus.PUBLISHED) {
            applyCounterDelta(review.placeId, rating = review.rating, cntDelta = 1, photoDelta = review.photoUrls.size)
        }

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
        val owned =
            (PlaceReviewTable.id eq reviewId) and
                (PlaceReviewTable.placeId eq placeId) and
                (PlaceReviewTable.userId eq userId) and
                PlaceReviewTable.deletedAt.isNull()
        // 삭제 전 상태·별점을 먼저 읽는다 — UPDATE … RETURNING 은 갱신 후 값(DELETED)만 돌려준다.
        val before =
            PlaceReviewTable
                .select(PlaceReviewTable.rating, PlaceReviewTable.status)
                .where { owned }
                .singleOrNull()
                ?: return 0
        val now = Clock.System.now()
        val updated =
            PlaceReviewTable.update({ owned }) {
                it[deletedAt] = now
                it[status] = PlaceReviewStatus.DELETED
                it[updatedAt] = now
            }
        if (updated == 0) return 0
        // 공개 리뷰였을 때만 카운터를 되돌린다 — 숨김(HIDDEN) 리뷰는 카운터에 들어 있지 않다.
        if (before[PlaceReviewTable.status] == PlaceReviewStatus.PUBLISHED) {
            val photoCount =
                PlaceReviewPhotoTable
                    .selectAll()
                    .where { PlaceReviewPhotoTable.placeReviewId eq reviewId }
                    .count()
                    .toInt()
            applyCounterDelta(
                placeId,
                rating = before[PlaceReviewTable.rating].toInt(),
                cntDelta = -1,
                photoDelta = -photoCount,
            )
        }
        return 1
    }

    /**
     * places 카운터 상대 갱신 — 리뷰 쓰기와 같은 트랜잭션에서만 부른다.
     * [cntDelta] 는 +1(작성)/-1(삭제)이고, 별점 합·별점별 수·사진 수를 그 방향으로 함께 움직인다.
     */
    private fun applyCounterDelta(
        placeId: Long,
        rating: Int,
        cntDelta: Int,
        photoDelta: Int,
    ) {
        val ratingColumn = PlaceTable.ratingCountColumn(rating)
        PlaceTable.update({ PlaceTable.id eq placeId }) {
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
        PlaceReviewPhotoTable.batchInsert(photoUrls.withIndex(), shouldReturnGeneratedValues = false) { (index, url) ->
            this[PlaceReviewPhotoTable.placeReviewId] = reviewId
            this[PlaceReviewPhotoTable.imageUrl] = url
            this[PlaceReviewPhotoTable.orderNo] = index.toShort()
        }
    }

    /** 태그 연결은 마스터 조회 없이 enum 이름을 그대로 심는다(태그 코드가 정본 — V6). */
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
