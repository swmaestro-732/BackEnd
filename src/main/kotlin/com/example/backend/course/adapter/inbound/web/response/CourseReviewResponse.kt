package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.dto.CourseReviewPageResult
import com.example.backend.course.application.dto.CourseReviewResult
import com.example.backend.course.application.dto.CourseReviewTagResult
import com.example.backend.course.application.dto.RatingCountResult
import java.time.OffsetDateTime

/**
 * 웹 응답 DTO — 코스 리뷰 한 페이지. 유스케이스 결과([CourseReviewPageResult])를 직렬화 형태로 변환한다.
 * averageRating/totalCount 는 코스 전체 집계, nextCursor/hasNext 는 커서 페이지 메타다.
 */
data class CourseReviewListResponse(
    val averageRating: Double,
    val totalCount: Int,
    val ratingDistribution: List<RatingCountResponse>,
    val hasCompletedCourse: Boolean,
    val nextCursor: String?,
    val hasNext: Boolean,
    val reviews: List<CourseReviewResponse>,
) {
    companion object {
        fun from(result: CourseReviewPageResult): CourseReviewListResponse =
            CourseReviewListResponse(
                averageRating = result.averageRating,
                totalCount = result.totalCount,
                ratingDistribution = result.ratingDistribution.map(RatingCountResponse::from),
                hasCompletedCourse = result.hasCompletedCourse,
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                reviews = result.reviews.map(CourseReviewResponse::from),
            )
    }
}

data class RatingCountResponse(
    val rating: Int,
    val count: Int,
) {
    companion object {
        fun from(result: RatingCountResult): RatingCountResponse =
            RatingCountResponse(rating = result.rating, count = result.count)
    }
}

data class CourseReviewResponse(
    val id: String,
    val authorId: Long,
    val rating: Int,
    val content: String,
    val createdAt: OffsetDateTime,
    val relativeTime: String,
    val photoUrls: List<String>,
    val tags: List<CourseReviewTagResponse>,
) {
    companion object {
        fun from(result: CourseReviewResult): CourseReviewResponse =
            CourseReviewResponse(
                id = result.id,
                authorId = result.authorId,
                rating = result.rating,
                content = result.content,
                createdAt = result.createdAt,
                relativeTime = result.relativeTime,
                photoUrls = result.photoUrls,
                tags = result.tags.map(CourseReviewTagResponse::from),
            )
    }
}

data class CourseReviewTagResponse(
    val label: String,
    val icon: String,
) {
    companion object {
        fun from(result: CourseReviewTagResult): CourseReviewTagResponse =
            CourseReviewTagResponse(label = result.label, icon = result.icon)
    }
}
