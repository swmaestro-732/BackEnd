package com.example.backend.course.application.port.inbound.dto

import com.example.backend.course.domain.model.CourseVisibility
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * 코스 생성 요청(모킹 API 계약) — 디자인(코스 만들기 1단계: 코스 정보 → 장소 담기 → 공개 설정)과
 * courses/course_places/course_place_images 스키마에서 도출한 필드.
 *
 * - tags: 태그 이름 목록. 추천 태그 응답([RecommendedTagsResponse])과 동일하게 이름 문자열 기반.
 * - isPublished: true 면 발행("코스 저장하기"), false 면 임시저장(빌더 상단 "임시저장" 버튼).
 * - 도보 시간은 서버가 자동 계산하므로 요청에 없다(디자인 "도보 9분 · 경로 자동").
 */
data class CreateCourseRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    val description: String?,
    val tags: List<String> = emptyList(),
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    @field:Valid
    val places: List<CreateCoursePlaceRequest> = emptyList(),
)

/** 코스에 담는 장소 한 곳. 사진은 필수이며 장소당 최대 6장(디자인 2/6 표시)이다. */
data class CreateCoursePlaceRequest(
    @field:Positive
    val placeId: Long,
    @field:Min(1)
    val orderNo: Int,
    @field:Size(max = 200)
    val caption: String?,
    @field:Size(min = 1, max = 6)
    val imageUrls: List<String>,
)
