package com.example.backend.course.domain.model

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * 코스 애그리거트 루트. 코스와 그에 담긴 장소([CoursePlace])·태그를 한 일관성 경계로 묶는다.
 * 신규 생성은 [create] 팩토리로, 저장된 상태 복원(영속 계층 → 도메인)은 [reconstitute] 팩토리로만 한다.
 * 팩토리 우회는 [ConsistentCopyVisibility] 로 차단한다(copy() 도 private).
 *
 * courses 테이블의 모든 컬럼을 필드로 보유한다. 생성 시점(insert 전)에 미정인 값은 nullable·기본값으로 두고,
 * DB 가 채우는 값(id·created_at·updated_at·카운터 등)은 [reconstitute] 로 되돌려 받아 채운다.
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class Course private constructor(
    val id: Long?,
    val userId: Long,
    val status: CourseStatus,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val category: CourseCategory?,
    val area: String?,
    val visitDate: LocalDate?,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val likesCnt: Int,
    val commentsCnt: Int,
    val savesCnt: Int,
    val tracingsCnt: Int,
    val forkedFromId: Long?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val deletedAt: Instant?,
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
            forkedFromId: Long?,
            tags: List<String>,
            places: List<CoursePlace>,
            placeCategoryByPlaceId: Map<Long, String>,
        ): Course =
            build(
                id = null,
                userId = userId,
                title = title,
                description = description,
                coverImageUrl = coverImageUrl,
                visibility = visibility,
                isPublished = isPublished,
                forkedFromId = forkedFromId,
                tags = tags,
                places = places,
                category = deriveCategory(isPublished, places, placeCategoryByPlaceId),
            )

        /**
         * 코스 편집(전체 치환). 이미 영속화된 코스([id])의 전체 상태를 요청 값으로 덮어쓴다.
         * 불변식은 [create] 와 동일하다. 카테고리는 생성과 달리 **서비스가 이미 해석해** 넘긴다([category]) —
         * 기존 카테고리가 있고 장소 구성이 그대로면 재도출 없이 유지하고, 그 외에만 [deriveCategory] 결과를 넘긴다.
         * 소유권·존재 여부는 서비스가 사전 검증한다.
         */
        fun edit(
            id: Long,
            userId: Long,
            title: String,
            description: String?,
            coverImageUrl: String?,
            visibility: CourseVisibility,
            isPublished: Boolean,
            tags: List<String>,
            places: List<CoursePlace>,
            category: CourseCategory?,
        ): Course =
            build(
                id = id,
                userId = userId,
                title = title,
                description = description,
                coverImageUrl = coverImageUrl,
                visibility = visibility,
                isPublished = isPublished,
                // 편집은 fork 원본을 바꾸지 않는다(update 도 forked_from_id 를 쓰지 않음) — 재구성용으로 null.
                forkedFromId = null,
                tags = tags,
                places = places,
                category = category,
            )

        /** 생성·편집 공통 — 도메인 불변식을 강제해 애그리거트를 만든다(카테고리는 호출부가 도출·결정). */
        private fun build(
            id: Long?,
            userId: Long,
            title: String,
            description: String?,
            coverImageUrl: String?,
            visibility: CourseVisibility,
            isPublished: Boolean,
            forkedFromId: Long?,
            tags: List<String>,
            places: List<CoursePlace>,
            category: CourseCategory?,
        ): Course {
            // 제목은 발행 코스만 필수다 — 임시저장(draft)은 제목 없이 저장할 수 있다(빌더 상단 "임시저장").
            if (isPublished && title.isBlank()) {
                throw BusinessException(ErrorCode.INVALID_INPUT, "코스를 발행하려면 제목이 필요합니다.")
            }
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
            // 생성 시점에 미정인 값은 pre-persist 기본값으로 둔다(id·타임스탬프는 DB 가, 카운터는 DB DEFAULT 가 채움).
            return Course(
                id = id,
                userId = userId,
                status = CourseStatus.ACTIVE,
                title = title,
                description = description,
                coverImageUrl = coverImageUrl,
                category = category,
                area = null,
                visitDate = null,
                visibility = visibility,
                isPublished = isPublished,
                likesCnt = 0,
                commentsCnt = 0,
                savesCnt = 0,
                tracingsCnt = 0,
                forkedFromId = forkedFromId,
                createdAt = null,
                updatedAt = null,
                deletedAt = null,
                tags = tags.map(String::trim).filter(String::isNotBlank).distinct(),
                places = places,
            )
        }

        /**
         * 영속 계층에서 읽어온 상태로 코스를 복원한다(insert 직후·조회 시). 이미 저장된 신뢰 값이라
         * 불변식 재검증·카테고리 재도출을 하지 않고 그대로 싣는다 — [create] 와 달리 파생 없이 전부 주입한다.
         * copy() 가 막혀 있어(팩토리 우회 차단) id·DB 생성값을 채운 [Course] 를 만드는 유일한 통로다.
         */
        @Suppress("LongParameterList")
        fun reconstitute(
            id: Long,
            userId: Long,
            status: CourseStatus,
            title: String,
            description: String?,
            coverImageUrl: String?,
            category: CourseCategory?,
            area: String?,
            visitDate: LocalDate?,
            visibility: CourseVisibility,
            isPublished: Boolean,
            likesCnt: Int,
            commentsCnt: Int,
            savesCnt: Int,
            tracingsCnt: Int,
            forkedFromId: Long?,
            createdAt: Instant?,
            updatedAt: Instant?,
            deletedAt: Instant?,
            tags: List<String>,
            places: List<CoursePlace>,
        ): Course =
            Course(
                id = id,
                userId = userId,
                status = status,
                title = title,
                description = description,
                coverImageUrl = coverImageUrl,
                category = category,
                area = area,
                visitDate = visitDate,
                visibility = visibility,
                isPublished = isPublished,
                likesCnt = likesCnt,
                commentsCnt = commentsCnt,
                savesCnt = savesCnt,
                tracingsCnt = tracingsCnt,
                forkedFromId = forkedFromId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                tags = tags,
                places = places,
            )

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
