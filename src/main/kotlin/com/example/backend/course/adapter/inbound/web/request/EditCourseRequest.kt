package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.domain.model.CourseVisibility
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** tags 테이블 name varchar(50) 과 동일한 제한. */
private const val MAX_TAG_LENGTH = 50

/**
 * 코스 편집 요청 — 웹 어댑터 DTO. 편집은 코스 만들기와 같은 빌더 화면을 재사용하므로
 * (코스 정보 → 장소 담기 → 공개 설정) 클라이언트가 편집 후 코스 전체 상태를 되돌려 보내는
 * **전체 치환(full replacement)** 계약이다 — 부분 패치가 아니라 보낸 필드로 코스를 덮어쓴다.
 * 필드 근거는 코스 생성([CreateCourseRequest])과 동일(노션 "코스 편집" 페이지는 필드 미작성 상태).
 *
 * 필드 형식·범위는 Bean Validation 으로 검증한다(→ 400 VALIDATION_FAILED + fieldErrors).
 * 교차 필드·비즈니스 규칙(발행 시 장소 최소 개수, orderNo 중복 금지)은 인바운드 포트(도메인)가 검증한다.
 *
 * - tags: 태그 이름 목록(추천 태그 응답과 동일하게 이름 문자열 기반).
 * - thumbnailUrl: 코스 커버 이미지(courses.cover_image_url).
 * - category 는 요청에 없다 — 담은 장소들의 카테고리로 서비스가 도출한다.
 * - isPublished: true 면 발행, false 면 임시저장 — 임시저장 코스를 편집하며 발행 전환하는 경우 포함.
 * - places 요소는 코스 생성과 동일한 [CreateCoursePlaceRequest] 를 재사용한다.
 */
data class EditCourseRequest(
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
    @field:Valid
    val places: List<CreateCoursePlaceRequest> = emptyList(),
) {
    fun toCommand(
        userId: Long,
        courseId: Long,
    ): EditCourseCommand =
        EditCourseCommand(
            courseId = courseId,
            userId = userId,
            title = title,
            description = description,
            coverImageUrl = thumbnailUrl,
            tags = tags,
            visibility = visibility,
            isPublished = isPublished,
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
