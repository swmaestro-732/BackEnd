package com.example.backend.course.application.port.inbound.dto

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CoursePlace

/**
 * 코스 생성 명령(애플리케이션 경계 타입). 웹 요청(CreateCourseRequest)에서 매핑된다.
 * coverImageUrl 은 요청의 thumbnailUrl(코스 커버). category 는 담은 장소들의 카테고리로 서비스가 도출한다.
 */
data class CreateCourseCommand(
    val userId: Long,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val tags: List<String>,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val forkedFromId: Long?,
    val places: List<CreateCoursePlaceCommand>,
)

data class CreateCoursePlaceCommand(
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    val imageUrls: List<String>,
    /** 다음 장소까지 도보 소요 시간(분). -1 은 도보 이동 불가, null 은 마지막 장소. */
    val walkingMinutes: Int?,
)

/**
 * 코스 포크 명령(애플리케이션 경계 타입). 웹 요청(ForkCourseRequest)에서 매핑된다.
 *
 * 포크가 원본에서 가져오는 것은 장소 구성(어디를 어떤 순서로)뿐이고 그 위의 콘텐츠(장소별 캡션·사진,
 * 제목·설명·커버·태그·공개 설정)는 포크하는 사람이 새로 입력하므로, 필드가 코스 생성과 같고
 * 원본 코스 [forkedFromId] 만 더 받는다(생성과 달리 **필수**).
 */
data class ForkCourseCommand(
    val userId: Long,
    val forkedFromId: Long,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val tags: List<String>,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val places: List<CreateCoursePlaceCommand>,
) {
    /** 원본 검증을 마친 뒤 실제 저장은 코스 생성 경로를 그대로 탄다(포크 표시는 forkedFromId 로 남는다). */
    fun toCreateCommand(): CreateCourseCommand =
        CreateCourseCommand(
            userId = userId,
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            tags = tags,
            visibility = visibility,
            isPublished = isPublished,
            forkedFromId = forkedFromId,
            places = places,
        )
}

/**
 * 코스 편집 명령(애플리케이션 경계 타입). 웹 요청(EditCourseRequest)에서 매핑된다.
 * 편집은 전체 치환(full replacement)이라 코스 생성과 필드가 같고, 대상 코스 [courseId] 와
 * 요청자 [userId](소유권 검증용)를 더 받는다. category 는 담은 장소들의 카테고리로 서비스가 도출한다.
 */
data class EditCourseCommand(
    val courseId: Long,
    val userId: Long,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val tags: List<String>,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val places: List<CreateCoursePlaceCommand>,
)

// ── 커맨드 → 도메인 매핑(순수) ──────────────────────────────────
// 포트 I/O(장소 존재 검증·지역명 조회)와 파생 결정(areaCode)은 서비스가 먼저 수행하고, 그 결과를 인자로 넘긴다.
// 여기서는 이미 구해진 값으로 도메인 애그리거트를 조립만 한다 — 그래서 순수하고 커맨드 옆에 둘 수 있다.

/** 커맨드 장소 목록 → 도메인 [CoursePlace] 매핑. */
fun List<CreateCoursePlaceCommand>.toCoursePlaces(): List<CoursePlace> =
    map {
        CoursePlace(
            placeId = it.placeId,
            orderNo = it.orderNo,
            caption = it.caption,
            imageUrls = it.imageUrls,
            walkingMinutes = it.walkingMinutes,
        )
    }

/** 생성 커맨드 → 도메인 [Course]. [places]·[areaCode]·[area] 는 서비스가 미리 도출·조회해 넘긴다. */
fun CreateCourseCommand.toCourse(
    places: List<CoursePlace>,
    foundPlaces: List<PlaceRef>,
    areaCode: String?,
    area: String?,
): Course =
    Course.create(
        userId = userId,
        title = title,
        description = description,
        coverImageUrl = coverImageUrl,
        visibility = visibility,
        isPublished = isPublished,
        forkedFromId = forkedFromId,
        tags = tags,
        places = places,
        placeCategoryByPlaceId = foundPlaces.associate { it.id to it.category },
        areaCode = areaCode,
        area = area,
    )

/** 편집 커맨드 → 도메인 [Course]. [places]·[areaCode]·[area] 는 서비스가 미리 도출·조회해 넘긴다. */
fun EditCourseCommand.toCourse(
    existing: CourseDetailRow,
    places: List<CoursePlace>,
    foundPlaces: List<PlaceRef>,
    areaCode: String?,
    area: String?,
): Course =
    Course.edit(
        id = courseId,
        userId = userId,
        title = title,
        description = description,
        coverImageUrl = coverImageUrl,
        visibility = visibility,
        isPublished = isPublished,
        tags = tags,
        places = places,
        wasPublished = existing.isPublished,
        existingCategory = existing.category,
        placeCategoryByPlaceId = foundPlaces.associate { it.id to it.category },
        areaCode = areaCode,
        area = area,
    )
