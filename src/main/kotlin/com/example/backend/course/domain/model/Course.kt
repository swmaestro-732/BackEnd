package com.example.backend.course.domain.model

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * 코스 애그리거트 루트.
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
    val areaCode: String?,
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
        /** 코스가 담아야 하는 최소 장소 수 — 발행·임시저장 공통. */
        private const val MIN_PLACES = 2

        /** 포크 시 원본 장소를 **전부** 유지해야 하는 원본 크기의 상한 — 이보다 크면 절반 이상만 유지하면 된다. */
        private const val FORK_FULL_KEEP_LIMIT = 4

        /**
         * 포크한 코스가 원본에서 **그대로 담아야 하는 최소 장소 수**. 장소 추가는 언제나 자유롭고,
         * 원본 장소를 빼는 것만 제한한다 — 포크가 원본과 다른 코스가 되어 버리는 것을 막는 규칙이다.
         * - 원본이 4곳 이하: 전부 유지(한 곳도 뺄 수 없다)
         * - 원본이 5곳 이상: 절반 이상 유지(올림 — 5곳이면 3곳, 6곳이면 3곳, 7곳이면 4곳)
         */
        fun requiredKeptPlaceCount(originPlaceCount: Int): Int =
            if (originPlaceCount <= FORK_FULL_KEEP_LIMIT) {
                originPlaceCount
            } else {
                (originPlaceCount + 1) / 2
            }

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
            areaCode: String?,
            area: String?,
        ): Course {
            val category = deriveCategory(isPublished, places, placeCategoryByPlaceId)
            return build(
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
                category = category,
                areaCode = areaCode,
                area = area,
            )
        }

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
            wasPublished: Boolean,
            existingCategory: CourseCategory?,
            placeCategoryByPlaceId: Map<Long, String>,
            areaCode: String?,
            area: String?,
        ): Course {
            val category =
                when {
                    !isPublished -> null
                    wasPublished && existingCategory != null -> existingCategory
                    else -> deriveCategory(true, places, placeCategoryByPlaceId)
                }

            return build(
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
                areaCode = areaCode,
                area = area,
            )
        }

        /** 생성·편집 공통 — 도메인 불변식을 강제해 애그리거트를 만든다(카테고리·지역코드는 호출부가 도출·결정). */
        @Suppress("LongParameterList")
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
            areaCode: String?,
            area: String?,
        ): Course {
            // 제목은 발행 코스만 필수다 — 임시저장(draft)은 제목 없이 저장할 수 있다(빌더 상단 "임시저장").
            if (isPublished && title.isBlank()) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "코스를 발행하려면 제목이 필요합니다.")
            }
            // 장소 최소 개수는 임시저장에도 적용된다 — 빌더에서 장소를 2곳 담아야 저장(임시저장 포함)할 수 있다.
            if (places.size < MIN_PLACES) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "코스에는 장소를 2곳 이상 담아야 합니다.")
            }
            if (isPublished && coverImageUrl.isNullOrBlank()) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "코스를 발행하려면 커버 이미지가 필요합니다.")
            }
            if (isPublished && places.any { it.imageUrls.isEmpty() }) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "발행 코스의 장소는 사진이 1장 이상이어야 합니다.")
            }
            if (places.map { it.orderNo }.toSet().size != places.size) {
                throw BusinessException(CommonErrorCode.INVALID_INPUT, "장소 순서(orderNo)가 중복되었습니다.")
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
                area = area,
                areaCode = areaCode,
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
            areaCode: String?,
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
                areaCode = areaCode,
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
         */
        private fun deriveCategory(
            isPublished: Boolean,
            places: List<CoursePlace>,
            placeCategoryByPlaceId: Map<Long, String>,
        ): CourseCategory? {
            if (!isPublished) return null
            val orderedNames = places.sortedBy { it.orderNo }.mapNotNull { placeCategoryByPlaceId[it.placeId] }
            return CourseCategory.fromPlaceCategoryNames(orderedNames)
        }

        /**
         * 코스 지역코드(법정동코드 10자리) 도출 규칙 — 발행 코스만 담은 장소들의 지역으로 정한다(임시저장은 null).
         * [deriveCategory] 와 같은 최빈값 규칙을 시군구(코드 앞 5자리) 레벨에 적용한다:
         * orderNo 순으로 장소의 법정동코드를 모아 최다 빈도 시군구를 고르고(동률은 앞선 장소 우선),
         * 그 시군구 안 장소들이 모두 같은 읍면동이면 동 코드(10자리)로 세분화, 아니면 시군구 코드 + "00000" 패딩.
         */
        fun deriveAreaCode(
            isPublished: Boolean,
            places: List<CoursePlace>,
            placeAreaCodeByPlaceId: Map<Long, String?>,
        ): String? {
            if (!isPublished) return null
            val orderedCodes = places.sortedBy { it.orderNo }.mapNotNull { placeAreaCodeByPlaceId[it.placeId] }
            if (orderedCodes.isEmpty()) return null

            val sigunguCounts = orderedCodes.groupingBy { it.take(SIGUNGU_CODE_LENGTH) }.eachCount()
            val maxCount = sigunguCounts.values.max()
            // orderNo 오름차순으로 순회하며 최다 빈도 시군구 중 먼저 나온 것 → 동률은 앞선 장소 우선(카테고리와 동일).
            val sigungu =
                orderedCodes.map { it.take(SIGUNGU_CODE_LENGTH) }.first { sigunguCounts.getValue(it) == maxCount }

            val dongCodes = orderedCodes.filter { it.startsWith(sigungu) }.distinct()
            return dongCodes.singleOrNull() ?: sigungu.padEnd(AREA_CODE_LENGTH, '0')
        }

        /** 법정동코드 자릿수 — 앞 5자리=시군구, 전체 10자리=읍면동(시군구 레벨은 뒤를 0 으로 패딩). */
        private const val SIGUNGU_CODE_LENGTH = 5
        private const val AREA_CODE_LENGTH = 10
    }
}
