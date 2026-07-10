package com.example.backend.course.application.dto

import java.time.OffsetDateTime

/**
 * 유스케이스 출력 — 코스 상세. 도메인 모델을 밖으로 노출하지 않기 위한 애플리케이션 경계 타입.
 * 프론트 상세 화면 계약에 맞춘 뷰 지향 구조다. id 는 외부 계약상 문자열로 다룬다.
 */
data class CourseDetailResult(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val themes: List<String>,
    val description: String,
    val stats: CourseStatsResult,
    val author: CourseAuthorResult,
    val places: List<CoursePlaceResult>,
    val reviewSummary: CourseReviewSummaryResult,
    val viewer: CourseViewerResult,
)

/** 코스 요약 지표. followerCountLabel 은 표시용 축약("1.2k"). */
data class CourseStatsResult(
    val placeCount: Int,
    val walkingMinutes: Int,
    val followerCount: Int,
    val followerCountLabel: String,
)

/** 코스 작성자. user 도메인 참조는 식별자(id)로만. isFollowing 은 조회자 기준. */
data class CourseAuthorResult(
    val id: String,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String,
    val isFollowing: Boolean,
)

/** 코스에 담긴 장소. place 도메인 참조는 식별자(id)로만. label 은 이전 장소로부터의 도보 시간 등. */
data class CoursePlaceResult(
    val order: Int,
    val id: String,
    val name: String,
    val categories: List<String>,
    val thumbnailUrl: String,
    val authorTip: String,
    val label: String?,
)

/** 리뷰 요약 — 평균/총개수 + 대표 리뷰 일부. */
data class CourseReviewSummaryResult(
    val averageRating: Double,
    val totalCount: Int,
    val reviews: List<CourseReviewResult>,
)

data class CourseReviewResult(
    val id: String,
    val author: CourseReviewAuthorResult,
    val rating: Int,
    val content: String,
    val createdAt: OffsetDateTime,
    val relativeTime: String,
    val photoUrls: List<String>,
)

data class CourseReviewAuthorResult(
    val nickname: String,
    val profileImageUrl: String,
)

/** 조회자 관점 상태(저장 여부/코스 시작 여부). */
data class CourseViewerResult(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)
