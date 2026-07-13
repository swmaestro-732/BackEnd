package com.example.backend.bff.adapter.inbound.web.response

import java.time.OffsetDateTime

/**
 * 웹 응답 DTO — 코스 상세 화면 조합(BFF). 프론트 화면 계약 형태.
 * 현재는 컨트롤러에서 목 데이터로 채운다(실제 구현 시 도메인 조합으로 교체).
 */
data class CourseDetailScreenResponse(
    val course: CourseScreenResponse,
    val reviewSummary: ReviewSummaryResponse,
)

data class CourseScreenResponse(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val themes: List<String>,
    val description: String,
    val stats: CourseStatsResponse,
    val author: AuthorResponse,
    val places: List<CoursePlaceScreenResponse>,
    val viewer: CourseViewerResponse,
)

/** 코스 작성자 카드 — 전체 프로필(팔로우 관계 포함). */
data class AuthorResponse(
    val id: Long,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String,
    val isFollowing: Boolean,
    val isFollower: Boolean,
)

data class CourseStatsResponse(
    val placeCount: Int,
    val walkingMinutes: Int,
    val tracingCountLabel: String,
)

data class CoursePlaceScreenResponse(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val name: String?,
    val caption: String?,
    val walkingMinutesToNext: Int?,
    val categories: List<String>,
    val images: List<CoursePlaceImageResponse>,
)

data class CoursePlaceImageResponse(
    val imageUrl: String,
    val orderNo: Int,
)

data class CourseViewerResponse(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)

/** 리뷰 섹션 — 코스 전체 집계 + 최신순 프리뷰. 전체 목록/페이징은 `/api/v1/courses/{id}/reviews` 로 조회한다. */
data class ReviewSummaryResponse(
    val averageRating: Double,
    val totalCount: Int,
    val hasCompletedCourse: Boolean,
    val ratingDistribution: List<RatingCountResponse>,
    val previews: List<ReviewPreviewResponse>,
)

data class RatingCountResponse(
    val rating: Int,
    val count: Int,
)

data class ReviewPreviewResponse(
    val id: String,
    val author: ReviewAuthorResponse?,
    val rating: Int,
    val content: String,
    val createdAt: OffsetDateTime,
    val relativeTime: String,
    val photoUrls: List<String>,
    val tags: List<ReviewTagResponse>,
)

/** 리뷰 작성자 — 간략 정보(닉네임·프로필 이미지). */
data class ReviewAuthorResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String,
)

data class ReviewTagResponse(
    val label: String,
    val icon: String,
)
