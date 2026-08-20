package com.example.backend.place.domain.model

import kotlin.time.Instant

/** 리뷰 사진 최대 개수 — 디자인(장소 리뷰 작성 "사진 추가 1/6"). 웹 요청 DTO 의 검증 상한과 같은 값이다. */
private const val MAX_PHOTOS = 6

/** 한 리뷰에 담을 수 있는 태그 최대 개수 — 디자인 태그 칩 그룹(업종별 6 + 공통) 기준 상한. */
private const val MAX_TAGS = 10

/** 한마디 남기기 최대 길이 — place_reviews.content(TEXT)에 여유를 둔 입력 상한. */
private const val MAX_CONTENT_LENGTH = 1000

/**
 * 장소 리뷰 애그리거트 — place_reviews 와 자식(place_review_photos·place_review_tag_links)을 한 덩어리로 든다.
 * 신규 작성은 [create] 팩토리로, 저장된 상태 복원(영속 계층 → 도메인)은 [reconstitute] 팩토리로만 한다.
 * 팩토리 우회는 [ConsistentCopyVisibility] 로 차단한다(copy() 도 private) — [Place] 와 같은 규칙이다.
 *
 * - [photoUrls] 순서가 곧 노출 순서(place_review_photos.order_no).
 * - [tags] 는 마스터 테이블 없이 코드로 저장한다([PlaceReviewTag]).
 * - 같은 사용자가 같은 장소에 여러 번 리뷰를 남길 수 있다(재방문마다 작성 — 유니크 제약을 두지 않는다).
 *
 * 생성 시점(insert 전)에 미정인 값(id·created_at)은 null 로 두고, DB 가 채운 값은 [reconstitute] 로 되돌려 받는다.
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
        /**
         * 신규 작성 — 도메인 불변식을 검증한다. 생성 시점에 미정인 값(id·created_at)은 null, 상태는 PUBLISHED.
         *
         * 웹 요청 DTO 의 Bean Validation 과 상한이 겹치지만(그쪽은 필드 단위 400 응답용),
         * 다른 진입점(BFF·배치)이 생겨도 규칙이 유지되도록 여기서도 검증한다.
         * 빈 한마디는 null 로 정규화하고, 같은 태그가 중복으로 오면 하나로 접는다(링크 테이블 PK 가 (리뷰, 태그)다).
         */
        fun create(
            placeId: Long,
            userId: Long,
            rating: Int,
            content: String?,
            photoUrls: List<String>,
            tags: List<PlaceReviewTag>,
        ): PlaceReview {
            require(rating in 1..5) { "별점은 1~5 사이여야 합니다." }
            require(photoUrls.size <= MAX_PHOTOS) { "사진은 최대 ${MAX_PHOTOS}장까지 올릴 수 있습니다." }
            require(photoUrls.none { it.isBlank() }) { "사진 URL 은 비어 있을 수 없습니다." }
            val normalizedContent = content?.trim()?.takeIf { it.isNotEmpty() }
            require((normalizedContent?.length ?: 0) <= MAX_CONTENT_LENGTH) {
                "한마디는 최대 ${MAX_CONTENT_LENGTH}자까지 쓸 수 있습니다."
            }
            val distinctTags = tags.distinct()
            require(distinctTags.size <= MAX_TAGS) { "태그는 최대 ${MAX_TAGS}개까지 고를 수 있습니다." }

            return PlaceReview(
                id = null,
                placeId = placeId,
                userId = userId,
                status = PlaceReviewStatus.PUBLISHED,
                rating = rating,
                content = normalizedContent,
                photoUrls = photoUrls,
                tags = distinctTags,
                createdAt = null,
            )
        }

        /**
         * 영속 저장소에서 읽어온 상태로 복원한다(이미 검증된 신뢰 값이라 불변식 재검증 없이 그대로 싣는다).
         * copy() 가 막혀 있어(팩토리 우회 차단) id·DB 생성값을 채운 [PlaceReview] 를 만드는 유일한 통로다.
         */
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
