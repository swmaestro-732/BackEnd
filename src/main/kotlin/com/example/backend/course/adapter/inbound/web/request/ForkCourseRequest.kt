package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.course.application.port.inbound.dto.ForkCourseCommand
import com.example.backend.course.domain.model.CourseVisibility
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** tags 테이블 name varchar(50) 과 동일한 제한. */
private const val MAX_TAG_LENGTH = 50

/** 코스 한 개에 담을 수 있는 장소 최대 개수(코스 생성과 동일). */
private const val MAX_PLACES = 10

/**
 * 코스 포크 요청 — 웹 어댑터 DTO. 노션 "코스 포크" 페이지가 본문 미작성이라
 * 기능 정의서(7.2 포크하기 → **7.2.1 게시물 만들기**)와 코스 생성 계약에서 도출했다.
 *
 * 포크가 원본에서 가져오는 것은 **장소 구성(어디를 어떤 순서로)** 뿐이고, 그 위에 올라가는 콘텐츠는
 * 전부 포크하는 사람이 새로 쓴다 — 장소별 caption·사진은 물론 제목·설명·커버·태그·공개 설정까지
 * 코스 만들기와 같은 빌더 화면에서 입력한다. 그래서 요청 모양이 코스 생성([CreateCourseRequest])·
 * 편집([EditCourseRequest])과 같고, 원본 id 는 바디가 아니라 경로(`{courseId}`)로 받는다
 * (실구현 시 courses.forked_from_id 로 저장 — 출처 표시용).
 *
 * 남의 사진·팁을 그대로 퍼가는 게 아니라 같은 코스를 자기 기록으로 다시 쓰는 것이므로,
 * 원본 콘텐츠를 승계하는 필드는 두지 않는다.
 *
 * 필드 형식·범위는 Bean Validation 으로 검증한다(→ 400 VALIDATION_FAILED + fieldErrors).
 * 교차 필드·비즈니스 규칙(장소 2곳 이상, orderNo 중복 금지, 발행 시 커버·장소마다 사진 1장 이상)은
 * 도메인([com.example.backend.course.domain.model.Course])이 검증한다(→ 400 INVALID_INPUT).
 *
 * - tags: 태그 이름 목록(추천 태그 응답과 동일하게 이름 문자열 기반).
 * - thumbnailUrl: 코스 커버 이미지(courses.cover_image_url).
 * - category 는 요청에 없다 — 담은 장소들의 카테고리로 서비스가 도출한다.
 * - isPublished: true 면 발행(바로 내 게시물), false 면 임시저장 — 포크 후 이어서 편집할 수 있다.
 * - visibility: 공개 범위. 원본 공개범위를 승계하지 않고 포크하는 사람이 정한다.
 * - places 요소는 코스 생성과 동일한 [CreateCoursePlaceRequest] 를 재사용한다 — placeId·orderNo 는
 *   원본에서 프리필되지만 caption·imageUrls 는 사용자가 새로 채운 값이 온다.
 *   **원본 장소를 일정 수 이상 그대로 담아야 한다**(원본 4곳 이하면 전부, 5곳 이상이면 절반 이상 —
 *   [com.example.backend.course.domain.model.Course.requiredKeptPlaceCount]). 장소 추가는 제한이 없다(최대 10곳).
 */
data class ForkCourseRequest(
    // 제목 필수 여부는 발행/임시저장에 따라 갈리는 비즈니스 규칙이라 도메인(Course)에서 검증한다
    // (임시저장은 제목 자체를 생략할 수 있고, 발행만 필수). 여기선 길이 제한(@Size)만 본다.
    @field:Size(max = 200)
    val title: String = "",
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
    @field:Size(max = MAX_PLACES)
    val places: List<CreateCoursePlaceRequest> = emptyList(),
) {
    fun toCommand(
        userId: Long,
        forkedFromId: Long,
    ): ForkCourseCommand =
        ForkCourseCommand(
            userId = userId,
            forkedFromId = forkedFromId,
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
                        walkingMinutes = it.walkingMinutes,
                    )
                },
        )
}
