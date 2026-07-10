package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.dto.CourseAuthorResult
import com.example.backend.course.application.dto.CourseDetailResult
import com.example.backend.course.application.dto.CoursePlaceResult
import com.example.backend.course.application.dto.CourseReviewAuthorResult
import com.example.backend.course.application.dto.CourseReviewResult
import com.example.backend.course.application.dto.CourseReviewSummaryResult
import com.example.backend.course.application.dto.CourseStatsResult
import com.example.backend.course.application.dto.CourseViewerResult
import java.time.OffsetDateTime

/**
 * 웹 응답 DTO. 유스케이스 결과([CourseDetailResult])를 직렬화 형태로 변환한다.
 * 프론트 계약대로 최상위를 { "course": {...} } 로 감싼다(공통 ApiResponse.data 안에 들어간다).
 */
data class CourseDetailResponse(
    val course: CourseResponse,
) {
    companion object {
        fun from(result: CourseDetailResult): CourseDetailResponse =
            CourseDetailResponse(course = CourseResponse.from(result))
    }
}

data class CourseResponse(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val themes: List<String>,
    val description: String,
    val stats: CourseStatsResponse,
    val author: CourseAuthorResponse,
    val places: List<CoursePlaceResponse>,
    val reviewSummary: CourseReviewSummaryResponse,
    val viewer: CourseViewerResponse,
) {
    companion object {
        fun from(result: CourseDetailResult): CourseResponse =
            CourseResponse(
                id = result.id,
                title = result.title,
                coverImageUrl = result.coverImageUrl,
                themes = result.themes,
                description = result.description,
                stats = CourseStatsResponse.from(result.stats),
                author = CourseAuthorResponse.from(result.author),
                places = result.places.map(CoursePlaceResponse::from),
                reviewSummary = CourseReviewSummaryResponse.from(result.reviewSummary),
                viewer = CourseViewerResponse.from(result.viewer),
            )
    }
}

data class CourseStatsResponse(
    val placeCount: Int,
    val walkingMinutes: Int,
    val followerCount: Int,
    val followerCountLabel: String,
) {
    companion object {
        fun from(result: CourseStatsResult): CourseStatsResponse =
            CourseStatsResponse(
                placeCount = result.placeCount,
                walkingMinutes = result.walkingMinutes,
                followerCount = result.followerCount,
                followerCountLabel = result.followerCountLabel,
            )
    }
}

data class CourseAuthorResponse(
    val id: String,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String,
    val isFollowing: Boolean,
) {
    companion object {
        fun from(result: CourseAuthorResult): CourseAuthorResponse =
            CourseAuthorResponse(
                id = result.id,
                nickname = result.nickname,
                handle = result.handle,
                profileImageUrl = result.profileImageUrl,
                isFollowing = result.isFollowing,
            )
    }
}

data class CoursePlaceResponse(
    val order: Int,
    val id: String,
    val name: String,
    val categories: List<String>,
    val thumbnailUrl: String,
    val authorTip: String,
    val label: String?,
) {
    companion object {
        fun from(result: CoursePlaceResult): CoursePlaceResponse =
            CoursePlaceResponse(
                order = result.order,
                id = result.id,
                name = result.name,
                categories = result.categories,
                thumbnailUrl = result.thumbnailUrl,
                authorTip = result.authorTip,
                label = result.label,
            )
    }
}

data class CourseReviewSummaryResponse(
    val averageRating: Double,
    val totalCount: Int,
    val reviews: List<CourseReviewResponse>,
) {
    companion object {
        fun from(result: CourseReviewSummaryResult): CourseReviewSummaryResponse =
            CourseReviewSummaryResponse(
                averageRating = result.averageRating,
                totalCount = result.totalCount,
                reviews = result.reviews.map(CourseReviewResponse::from),
            )
    }
}

data class CourseReviewResponse(
    val id: String,
    val author: CourseReviewAuthorResponse,
    val rating: Int,
    val content: String,
    val createdAt: OffsetDateTime,
    val relativeTime: String,
    val photoUrls: List<String>,
) {
    companion object {
        fun from(result: CourseReviewResult): CourseReviewResponse =
            CourseReviewResponse(
                id = result.id,
                author = CourseReviewAuthorResponse.from(result.author),
                rating = result.rating,
                content = result.content,
                createdAt = result.createdAt,
                relativeTime = result.relativeTime,
                photoUrls = result.photoUrls,
            )
    }
}

data class CourseReviewAuthorResponse(
    val nickname: String,
    val profileImageUrl: String,
) {
    companion object {
        fun from(result: CourseReviewAuthorResult): CourseReviewAuthorResponse =
            CourseReviewAuthorResponse(nickname = result.nickname, profileImageUrl = result.profileImageUrl)
    }
}

data class CourseViewerResponse(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
) {
    companion object {
        fun from(result: CourseViewerResult): CourseViewerResponse =
            CourseViewerResponse(hasSaved = result.hasSaved, hasStartedCourse = result.hasStartedCourse)
    }
}
