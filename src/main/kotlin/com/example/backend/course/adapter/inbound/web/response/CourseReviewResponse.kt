package com.example.backend.course.adapter.inbound.web.response

import java.time.OffsetDateTime

/**
 * 웹 응답 DTO — 코스 리뷰 한 페이지.
 * averageRating/totalCount 는 코스 전체 집계, nextCursor/hasNext 는 커서 페이지 메타다.
 */
data class CourseReviewListResponse(
    val averageRating: Double,
    val totalCount: Int,
    val ratingDistribution: List<RatingCountResponse>,
    /** 조회자가 이 코스를 완주(트레이싱 완료)했는지. 리뷰 작성 가능 여부 노출 등에 사용. */
    val hasCompletedCourse: Boolean,
    val nextCursor: String?,
    val hasNext: Boolean,
    val reviews: List<CourseReviewResponse>,
)

/** 별점별 리뷰 개수. 5~1점 각각 항상 존재(해당 별점이 없으면 count=0). */
data class RatingCountResponse(
    val rating: Int,
    val count: Int,
)

data class CourseReviewResponse(
    val id: String,
    val authorId: Long,
    val rating: Int,
    val content: String,
    val createdAt: OffsetDateTime,
    val relativeTime: String,
    val photoUrls: List<String>,
    val tags: List<CourseReviewTagResponse>,
)

/** 리뷰 태그 — label(예: "구성이 알차요") + icon 키워드(예: "packed", 프론트가 아이콘으로 매핑). */
data class CourseReviewTagResponse(
    val label: String,
    val icon: String,
)
