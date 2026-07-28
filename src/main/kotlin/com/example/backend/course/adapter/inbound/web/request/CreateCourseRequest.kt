package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.course.domain.model.CourseVisibility
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/** tags 테이블 name varchar(50) 과 동일한 제한. */
private const val MAX_TAG_LENGTH = 50

/** 코스 한 개에 담을 수 있는 장소 최대 개수. */
private const val MAX_PLACES = 10

/**
 * 코스 생성 요청(모킹 API) — 웹 어댑터 DTO. 디자인(코스 만들기 1단계: 코스 정보 → 장소 담기 → 공개 설정)과
 * courses/course_places/course_place_images 스키마에서 도출한 필드.
 *
 * 필드 형식·범위는 Bean Validation 으로 검증한다(→ 400 VALIDATION_FAILED + fieldErrors).
 * 교차 필드·비즈니스 규칙(발행 시 장소 1곳 이상, orderNo 중복 금지)은 애노테이션으로 표현할 수 없어
 * 컨트롤러가 직접 검증한다(→ 400 INVALID_INPUT).
 *
 * - tags: 태그 이름 목록. 추천 태그 응답(RecommendedTagsResponse)과 동일하게 이름 문자열 기반.
 * - thumbnailUrl: 코스 커버 이미지(courses.cover_image_url).
 * - category 는 요청에 없다 — 담은 장소들의 카테고리로 서비스가 도출한다(CourseCategory.fromPlaceCategoryNames).
 * - isPublished: true 면 발행("코스 저장하기"), false 면 임시저장(빌더 상단 "임시저장" 버튼).
 * - 도보 시간은 서버가 자동 계산하므로 요청에 없다(디자인 "도보 9분 · 경로 자동").
 */
data class CreateCourseRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val tags: List<
        @NotBlank
        @Size(max = MAX_TAG_LENGTH)
        String,
    > = emptyList(),
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    /** 포크(다른 코스에서 복제) 원본 course id. 일반 생성이면 요청에 없거나 null. */
    @field:Positive
    val forkedFromId: Long? = null,
    @field:Valid
    @field:Size(max = MAX_PLACES)
    val places: List<CreateCoursePlaceRequest> = emptyList(),
) {
    fun toCommand(userId: Long): CreateCourseCommand =
        CreateCourseCommand(
            userId = userId,
            title = title,
            description = description,
            coverImageUrl = thumbnailUrl,
            tags = tags,
            visibility = visibility,
            isPublished = isPublished,
            forkedFromId = forkedFromId,
            places =
                places.map {
                    CreateCoursePlaceCommand(
                        placeId = it.placeId,
                        orderNo = it.orderNo,
                        caption = it.caption,
                        imageUrls = it.imageUrls,
                    )
                },
        )
}

/**
 * 코스에 담는 장소 한 곳. orderNo 는 0부터 시작한다.
 * 사진 최소 1장 규칙은 발행 여부에 따라 달라지는 비즈니스 규칙이라 도메인([com.example.backend.course.domain.model.Course])에서
 * 검증한다(발행은 장소마다 1장 이상, 임시저장은 0장 허용). 여기선 URL 형식(@NotBlank)만 본다.
 */
data class CreateCoursePlaceRequest(
    @field:Positive
    val placeId: Long,
    @field:Min(0)
    val orderNo: Int,
    @field:Size(max = 200)
    val caption: String?,
    val imageUrls: List<@NotBlank String>,
)
