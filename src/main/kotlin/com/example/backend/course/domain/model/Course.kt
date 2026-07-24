package com.example.backend.course.domain.model

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode

/**
 * 코스 애그리거트 루트. 코스와 그에 담긴 장소([CoursePlace])·태그를 한 일관성 경계로 묶는다.
 * 생성은 [create] 팩토리로만 하며, 여기서 도메인 불변식을 강제한다(팩토리 우회는 [ConsistentCopyVisibility] 로 차단).
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class Course private constructor(
    val id: Long?,
    val userId: Long,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val category: CourseCategory?,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val tags: List<String>,
    val places: List<CoursePlace>,
) {
    companion object {
        /** 발행 코스가 담아야 하는 최소 장소 수(임시저장은 제한 없음). */
        private const val MIN_PUBLISHED_PLACES = 2

        fun create(
            userId: Long,
            title: String,
            description: String?,
            coverImageUrl: String?,
            visibility: CourseVisibility,
            isPublished: Boolean,
            tags: List<String>,
            places: List<CoursePlace>,
            placeCategoryByPlaceId: Map<Long, String>,
        ): Course {
            require(title.isNotBlank()) { "제목은 비어 있을 수 없습니다." }
            if (isPublished && places.size < MIN_PUBLISHED_PLACES) {
                throw BusinessException(ErrorCode.INVALID_INPUT, "코스를 발행하려면 장소를 2곳 이상 담아야 합니다.")
            }
            if (isPublished && coverImageUrl.isNullOrBlank()) {
                throw BusinessException(ErrorCode.INVALID_INPUT, "코스를 발행하려면 커버 이미지가 필요합니다.")
            }
            if (isPublished && places.any { it.imageUrls.isEmpty() }) {
                throw BusinessException(ErrorCode.INVALID_INPUT, "발행 코스의 장소는 사진이 1장 이상이어야 합니다.")
            }
            if (places.map { it.orderNo }.toSet().size != places.size) {
                throw BusinessException(ErrorCode.INVALID_INPUT, "장소 순서(orderNo)가 중복되었습니다.")
            }
            return Course(
                id = null,
                userId = userId,
                title = title,
                description = description,
                coverImageUrl = coverImageUrl,
                category = deriveCategory(isPublished, places, placeCategoryByPlaceId),
                visibility = visibility,
                isPublished = isPublished,
                tags = tags.map(String::trim).filter(String::isNotBlank).distinct(),
                places = places,
            )
        }

        /**
         * 코스 카테고리 도출 규칙 — 발행 코스만 담은 장소들의 카테고리로 정한다(임시저장은 null).
         * 장소를 orderNo 순으로 정렬해 각 장소의 카테고리(placeId→PlaceCategory 이름)를 모으고,
         * [CourseCategory.fromPlaceCategoryNames] 로 최빈값을 고른다.
         *
         * 생성-시-발행과 draft→발행 전이가 동일 규칙을 쓰도록 도메인에 둔다(placeCategoryByPlaceId 는 place 도메인에서 조회해 주입).
         */
        fun deriveCategory(
            isPublished: Boolean,
            places: List<CoursePlace>,
            placeCategoryByPlaceId: Map<Long, String>,
        ): CourseCategory? {
            if (!isPublished) return null
            val orderedNames = places.sortedBy { it.orderNo }.mapNotNull { placeCategoryByPlaceId[it.placeId] }
            return CourseCategory.fromPlaceCategoryNames(orderedNames)
        }
    }
}
