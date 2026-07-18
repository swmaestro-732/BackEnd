package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.domain.model.CourseVisibility
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/** tags 테이블 name varchar(50) 과 동일한 제한. */
private const val MAX_TAG_LENGTH = 50

/**
 * 코스 생성 요청(모킹 API) — 웹 어댑터 DTO. 디자인(코스 만들기 1단계: 코스 정보 → 장소 담기 → 공개 설정)과
 * courses/course_places/course_place_images 스키마에서 도출한 필드.
 *
 * 필드 형식·범위는 Bean Validation 으로 검증한다(→ 400 VALIDATION_FAILED + fieldErrors).
 * 교차 필드·비즈니스 규칙(발행 시 장소 1곳 이상, orderNo 중복 금지)은 애노테이션으로 표현할 수 없어
 * 컨트롤러가 직접 검증한다(→ 400 INVALID_INPUT).
 *
 * - tags: 태그 이름 목록. 추천 태그 응답(RecommendedTagsResponse)과 동일하게 이름 문자열 기반.
 * - isPublished: true 면 발행("코스 저장하기"), false 면 임시저장(빌더 상단 "임시저장" 버튼).
 * - 도보 시간은 서버가 자동 계산하므로 요청에 없다(디자인 "도보 9분 · 경로 자동").
 *
 * 실구현 시 애플리케이션 경계 타입(CreateCourseCommand)으로 매핑하는 `toCommand()` 를 추가한다.
 */
data class CreateCourseRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    val description: String?,
    val tags: List<
        @NotBlank
        @Size(max = MAX_TAG_LENGTH)
        String,
    > = emptyList(),
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    @field:Valid
    val places: List<CreateCoursePlaceRequest> = emptyList(),
)

/** 코스에 담는 장소 한 곳. 사진은 1장 이상 필수(상한 없음), orderNo 는 0부터 시작한다. */
data class CreateCoursePlaceRequest(
    @field:Positive
    val placeId: Long,
    @field:Min(0)
    val orderNo: Int,
    @field:Size(max = 200)
    val caption: String?,
    @field:Size(min = 1)
    val imageUrls: List<@NotBlank String>,
)
