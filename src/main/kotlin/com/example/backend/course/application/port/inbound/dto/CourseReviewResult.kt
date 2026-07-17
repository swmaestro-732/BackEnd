package com.example.backend.course.application.port.inbound.dto

import java.time.OffsetDateTime

/**
 * 유스케이스 출력 — 코스 리뷰 한 페이지.
 * averageRating/totalCount 는 코스 전체 집계(페이지와 무관).
 * nextCursor 는 다음 페이지 요청에 넘길 불투명 토큰이며, 다음 페이지가 없으면 null 이다.
 */
data class CourseReviewPageResult(
    val averageRating: Double,
    val totalCount: Int,
    val ratingDistribution: List<RatingCountResult>,
    /** 조회자가 이 코스를 완주(트레이싱 완료)했는지. 리뷰 작성 가능 여부 노출 등에 사용. */
    val hasCompletedCourse: Boolean,
    val nextCursor: String?,
    val hasNext: Boolean,
    val reviews: List<CourseReviewResult>,
)

/** 별점별 리뷰 개수. 5~1점 각각 항상 존재(해당 별점이 없으면 count=0). */
data class RatingCountResult(
    val rating: Int,
    val count: Int,
)

data class CourseReviewResult(
    val id: String,
    val authorId: Long,
    val rating: Int,
    val content: String,
    val createdAt: OffsetDateTime,
    val relativeTime: String,
    val photoUrls: List<String>,
    val tags: List<CourseReviewTagResult>,
)

/** 리뷰 태그 — label(예: "구성이 알차요") + icon 키워드(예: "packed", 프론트가 아이콘으로 매핑). */
data class CourseReviewTagResult(
    val label: String,
    val icon: String,
)
