package com.example.backend.place.domain.model

import kotlin.time.Instant

/**
 * 장소 리뷰 애그리거트
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class PlaceReview private constructor(
    val id: Long?,
    val placeId: Long,
    val userId: Long,
    val status: PlaceReviewStatus,
    val rating: Int,
    val content: String?,
    val photoUrls: List<String>,
    val tags: List<PlaceReviewTag>,
    val createdAt: Instant?,
) {
    companion object {
        const val MAX_PHOTOS = 6
        const val MAX_TAGS = 5
        const val MAX_CONTENT_LENGTH = 1000

        fun create(
            placeId: Long,
            userId: Long,
            rating: Int,
            content: String?,
            photoUrls: List<String>,
            tags: Set<PlaceReviewTag>,
        ): PlaceReview {
            require(rating in 1..5) { "별점은 1~5 사이여야 합니다." }
            require(photoUrls.size <= MAX_PHOTOS) { "사진은 최대 ${MAX_PHOTOS}장까지 올릴 수 있습니다." }
            require(photoUrls.none { it.isBlank() }) { "사진 URL 은 비어 있을 수 없습니다." }
            val normalizedContent = content?.trim()?.takeIf { it.isNotEmpty() }
            require((normalizedContent?.length ?: 0) <= MAX_CONTENT_LENGTH) {
                "한마디는 최대 ${MAX_CONTENT_LENGTH}자까지 쓸 수 있습니다."
            }
            require(tags.size <= MAX_TAGS) { "태그는 최대 ${MAX_TAGS}개까지 고를 수 있습니다." }

            return PlaceReview(
                id = null,
                placeId = placeId,
                userId = userId,
                status = PlaceReviewStatus.PUBLISHED,
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
            placeId: Long,
            userId: Long,
            status: PlaceReviewStatus,
            rating: Int,
            content: String?,
            photoUrls: List<String>,
            tags: List<PlaceReviewTag>,
            createdAt: Instant?,
        ): PlaceReview =
            PlaceReview(
                id = id,
                placeId = placeId,
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
