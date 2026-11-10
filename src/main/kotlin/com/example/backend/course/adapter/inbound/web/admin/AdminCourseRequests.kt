package com.example.backend.course.adapter.inbound.web.admin

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.inbound.web.request.CreateCoursePlaceRequest
import com.example.backend.course.application.port.inbound.dto.AdminEditCourseCommand
import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.course.domain.model.CourseCategory
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

private const val MAX_TAG_LENGTH = 50
private const val MAX_PLACES = 10
private const val MAX_BULK_IDS = 50

/**
 * 어드민 코스 수정: 사용자 PATCH([com.example.backend.course.adapter.inbound.web.request.EditCourseRequest])와 같은 본문에
 * 상세 응답의 updatedAt 을 그대로 담은 [expectedUpdatedAt] 을 더한다(다르면 409).
 */
data class AdminEditCourseRequest(
    val expectedUpdatedAt: Instant,
    @field:Size(max = 200)
    val title: String = "",
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val tags: List<
        @NotBlank
        @Size(max = MAX_TAG_LENGTH)
        String,
    > = emptyList(),
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val companions: List<CourseCategory>? = null,
    @field:Valid
    @field:Size(max = MAX_PLACES)
    val places: List<CreateCoursePlaceRequest> = emptyList(),
) {
    fun toCommand(
        adminEmail: String,
        courseId: Long,
    ) = AdminEditCourseCommand(
        adminEmail = adminEmail,
        courseId = courseId,
        expectedUpdatedAt = expectedUpdatedAt,
        title = title,
        description = description,
        coverImageUrl = thumbnailUrl,
        tags = tags,
        visibility = visibility,
        isPublished = isPublished,
        companions = companions,
        places =
            places.map {
                CreateCoursePlaceCommand(it.placeId, it.orderNo, it.caption, it.imageUrls, it.walkingMinutes)
            },
    )
}

enum class AdminCourseBulkAction {
    HIDE,
    UNHIDE,
    DELETE,
}

data class AdminCourseBulkRequest(
    @field:Size(min = 1, max = MAX_BULK_IDS)
    val ids: List<Long>,
    val action: AdminCourseBulkAction,
)
