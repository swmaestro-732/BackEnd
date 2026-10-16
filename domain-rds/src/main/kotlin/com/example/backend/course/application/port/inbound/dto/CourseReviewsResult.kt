package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/**
 * 인바운드 포트 반환 DTO — 코스 리뷰 한 페이지 + 코스 전체 집계.
 * 작성자는 id([CourseReviewItem.userId])만 담는다 — 닉네임·프로필 이미지 병합은 화면 조합(BFF)의 몫이다.
 *
 * - [averageRating] 살아있는 리뷰의 평점 평균(소수 첫째 자리 반올림). 리뷰가 없으면 0.0.
 * - [totalCount] 리뷰 총 개수(페이지가 아니라 코스 전체).
 * - [ratingDistribution] 5~1점 각각의 개수(없는 별점도 count=0 으로 항상 채운다).
 * - [photoCount] 이 코스 리뷰에 달린 사진 총 개수.
 * - [nextCursor]·[hasNext] 커서 페이지 메타.
 */
data class CourseReviewsResult(
    val averageRating: Double,
    val totalCount: Int,
    val ratingDistribution: List<RatingCount>,
    val photoCount: Int,
    val nextCursor: String?,
    val hasNext: Boolean,
    val reviews: List<CourseReviewItem>,
) {
    /** 별점별 리뷰 개수. */
    data class RatingCount(
        val rating: Int,
        val count: Int,
    )

    /** 리뷰 한 건. 사진은 노출 순서(order_no)대로 담긴다. */
    data class CourseReviewItem(
        val id: Long,
        val userId: Long,
        val rating: Int,
        val content: String?,
        val createdAt: Instant,
        val photoUrls: List<String>,
        val tags: List<Tag>,
    )

    /**
     * 리뷰 태그 — 코드(`.ai/taxonomy.md` 키워드) + 문구 + 이모지.
     * 도메인 enum(CourseReviewTag)은 경계 밖으로 내보내지 않고 표시 값만 옮긴다.
     */
    data class Tag(
        val code: String,
        val label: String,
        val icon: String,
    )
}
