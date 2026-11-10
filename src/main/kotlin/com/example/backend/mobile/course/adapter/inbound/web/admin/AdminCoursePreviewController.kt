package com.example.backend.mobile.course.adapter.inbound.web.admin

import com.example.backend.common.response.ApiResponse
import com.example.backend.course.application.port.inbound.dto.CoursePreviewCommand
import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseDetailScreenResponse
import com.example.backend.mobile.course.application.port.inbound.CourseMobileUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터: 어드민 코스 미리보기(SCRUM-752). 앱 코스 상세(`GET /service/v1/courses/{id}`)와 같은 응답 모양이라
 * 어드민 웹이 앱 웹의 상세 컴포넌트로 그대로 그린다. course 가 mobile 을 모르도록 BFF 모듈에 둔다.
 * /admin/v1 경로라 어드민 토큰만 통과한다(SecurityConfig). 저장, 조회수, 외부 도보 API, presigned 발급이 없다.
 */
@RestController
@RequestMapping("/admin/v1/course-previews")
class AdminCoursePreviewController(
    private val courseMobileUseCase: CourseMobileUseCase,
) {
    @PostMapping
    fun preview(
        @Valid @RequestBody request: CoursePreviewRequest,
    ): ApiResponse<CourseDetailScreenResponse> =
        ApiResponse.success(CourseDetailScreenResponse.from(courseMobileUseCase.getPreviewScreen(request.toCommand())))
}

/** 미리보기 초안: 어드민 PATCH 본문과 같은 필드(thumbnailUrl 등)에 표시용 courseId, authorId 를 더한다(둘 다 선택). */
data class CoursePreviewRequest(
    val courseId: Long? = null,
    val authorId: Long? = null,
    @field:Size(max = 200)
    val title: String = "",
    @field:Size(max = MAX_DESCRIPTION)
    val description: String? = null,
    val thumbnailUrl: String? = null,
    /** DATE, FAMILY, SOLO 중(BFF 는 course 도메인 enum 을 참조하지 않아 이름으로 받는다). */
    @field:Size(max = MAX_COMPANIONS)
    val companions: List<String> = emptyList(),
    @field:Size(max = MAX_TAGS)
    val tags: List<String> = emptyList(),
    @field:Valid
    @field:Size(max = MAX_PLACES)
    val places: List<CoursePreviewPlaceRequest> = emptyList(),
) {
    fun toCommand() =
        CoursePreviewCommand(
            courseId = courseId,
            authorId = authorId,
            title = title,
            description = description,
            coverImageUrl = thumbnailUrl,
            companions = companions,
            tags = tags,
            places =
                places.map {
                    CreateCoursePlaceCommand(
                        it.placeId,
                        it.orderNo,
                        it.caption,
                        it.imageUrls,
                        it.walkingMinutes,
                    )
                },
        )

    private companion object {
        const val MAX_PLACES = 10
        const val MAX_DESCRIPTION = 2000
        const val MAX_COMPANIONS = 10
        const val MAX_TAGS = 30
    }
}

/** 도보 시간은 입력값을 그대로 보여 준다(null 이면 비움). */
data class CoursePreviewPlaceRequest(
    @field:Positive
    val placeId: Long,
    @field:Min(0)
    val orderNo: Int,
    val caption: String? = null,
    @field:Size(max = 10)
    val imageUrls: List<String> = emptyList(),
    val walkingMinutes: Int? = null,
)
