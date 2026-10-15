package com.example.backend.course.domain.model

import kotlin.time.Instant

/**
 * 코스 리뷰 애그리거트
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class CourseReview private constructor(
    val id: Long?,
    val courseId: Long,
    val userId: Long,
    val status: CourseReviewStatus,
    val rating: Int,
    val content: String?,
    val photoUrls: List<String>,
    val tags: List<CourseReviewTag>,
    val createdAt: Instant?,
) {
    companion object {
        const val MAX_PHOTOS = 6
        const val MAX_TAGS = 5
        const val MAX_CONTENT_LENGTH = 1000

        fun create(
            courseId: Long,
            userId: Long,
            rating: Int,
            content: String?,
            photoUrls: List<String>,
            tags: Set<CourseReviewTag>,
        ): CourseReview {
            require(rating in 1..5) { "별점은 1~5 사이여야 합니다." }
            require(photoUrls.size <= MAX_PHOTOS) { "사진은 최대 ${MAX_PHOTOS}장까지 올릴 수 있습니다." }
            require(photoUrls.none { it.isBlank() }) { "사진 URL 은 비어 있을 수 없습니다." }
            val normalizedContent = content?.trim()?.takeIf { it.isNotEmpty() }
            require((normalizedContent?.length ?: 0) <= MAX_CONTENT_LENGTH) {
                "한마디는 최대 ${MAX_CONTENT_LENGTH}자까지 쓸 수 있습니다."
            }
            require(tags.size <= MAX_TAGS) { "태그는 최대 ${MAX_TAGS}개까지 고를 수 있습니다." }

            return CourseReview(
                id = null,
                courseId = courseId,
                userId = userId,
                status = CourseReviewStatus.PUBLISHED,
                rating = rating,
                content = normalizedContent,
                photoUrls = photoUrls,
                tags = tags.toList(),
                createdAt = null,
            )
        }

        @Suppress("LongParameterList")
        fun reconstitute(
            id: Long,
            courseId: Long,
            userId: Long,
            status: CourseReviewStatus,
            rating: Int,
            content: String?,
            photoUrls: List<String>,
            tags: List<CourseReviewTag>,
            createdAt: Instant?,
        ): CourseReview =
            CourseReview(
                id = id,
                courseId = courseId,
                userId = userId,
                status = status,
                rating = rating,
                content = content,
                photoUrls = photoUrls,
                tags = tags,
                createdAt = createdAt,
            )
    }
}
