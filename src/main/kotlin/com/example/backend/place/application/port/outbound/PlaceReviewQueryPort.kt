package com.example.backend.place.application.port.outbound

import com.example.backend.place.application.port.inbound.dto.PlaceReviewSortKey
import com.example.backend.place.domain.model.PlaceReviewTag
import java.time.Instant

/**
 * 아웃바운드 포트 — 장소 리뷰(place_reviews) 읽기 계약.
 * 모든 조회는 살아있는(deleted_at IS NULL, status=PUBLISHED) 리뷰만 대상으로 한다.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface PlaceReviewQueryPort {
    /**
     * 리뷰 한 페이지를 정렬 기준대로 읽는다. [cursor] 가 있으면 그 뒤부터, [limit] 개까지.
     * 자식(사진·태그)은 담지 않는다 — 항목별 조회(N+1)를 피하려고 [findPhotoUrls]·[findTags] 로 따로 모은다.
     */
    fun findReviewsByPlace(
        placeId: Long,
        sort: PlaceReviewSortKey,
        descending: Boolean,
        cursor: PlaceReviewCursor?,
        limit: Int,
    ): List<PlaceReviewRow>

    /** 리뷰 id 별 사진 URL — 노출 순서(order_no) 오름차순. */
    fun findPhotoUrls(reviewIds: List<Long>): Map<Long, List<String>>

    /** 리뷰 id 별 태그. 저장된 값이 곧 enum 이름이라 마스터 조회가 없다(V5). */
    fun findTags(reviewIds: List<Long>): Map<Long, List<PlaceReviewTag>>

    /** 장소 전체의 별점별 리뷰 수. 실제 존재하는 별점만 반환하며 페이지와 무관하다. */
    fun countReviewsByRating(placeId: Long): Map<Int, Long>

    /** 장소 전체의 살아있는 리뷰에 달린 사진 수. 페이지와 무관하다. */
    fun countPhotosByPlace(placeId: Long): Long
}

/** 리뷰 읽기 모델 — 자식(사진·태그) 없이 본문만. */
data class PlaceReviewRow(
    val id: Long,
    val userId: Long,
    val rating: Int,
    val content: String?,
    val createdAt: Instant,
)

/**
 * 커서 키셋 — 정렬 기준값과 함께 (작성일, id)까지 담아 동점에서도 페이지 경계가 흔들리지 않게 한다.
 * [rating] 은 평점 정렬일 때만 쓴다(최신순이면 무시).
 */
data class PlaceReviewCursor(
    val rating: Int,
    val createdAt: Instant,
    val id: Long,
)
