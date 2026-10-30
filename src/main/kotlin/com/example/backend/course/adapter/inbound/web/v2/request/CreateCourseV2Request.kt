package com.example.backend.course.adapter.inbound.web.v2.request

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.inbound.web.request.CreateCoursePlaceRequest
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.course.domain.model.CourseCategory
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/** tags 테이블 name varchar(50) 과 동일한 제한. */
private const val MAX_TAG_LENGTH = 50

/** 코스 한 개에 담을 수 있는 장소 최대 개수. */
private const val MAX_PLACES = 10

/**
 * 코스 생성 요청 v2. v1([com.example.backend.course.adapter.inbound.web.request.CreateCourseRequest])과 필드가 같고,
 * 카테고리를 장소로 도출하지 않고 작성자가 고른 [categories](개수 제한 없음, 중복은 도메인이 합침)로 받는다.
 */
data class CreateCourseV2Request(
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
    val categories: List<CourseCategory>,
    @field:Positive
    val duplicatedFromId: Long? = null,
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
            categories = categories,
            duplicatedFromId = duplicatedFromId,
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
