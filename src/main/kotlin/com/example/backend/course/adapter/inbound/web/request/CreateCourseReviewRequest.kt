package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.inbound.dto.CreateCourseReviewCommand
import com.example.backend.course.domain.model.CourseReview
import com.example.backend.course.domain.model.CourseReviewTag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 코스 리뷰 생성 요청 — 웹 어댑터 DTO. */
data class CreateCourseReviewRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    val content: String? = null,
    @field:Size(max = CourseReview.MAX_PHOTOS)
    val photoUrls: List<
        @NotBlank
        String,
    > = emptyList(),
    @field:Size(max = CourseReview.MAX_TAGS)
    val tagCodes: List<
        @NotBlank
        String,
    > = emptyList(),
) {
    fun toCommand(
        courseId: Long,
        userId: Long,
    ): CreateCourseReviewCommand =
        CreateCourseReviewCommand(
            courseId = courseId,
            userId = userId,
            rating = rating,
            content = content,
            photoUrls = photoUrls,
            tags =
                tagCodes
                    .map { code ->
                        CourseReviewTag.fromCodeOrNull(code)
                            ?: throw BusinessException(CommonErrorCode.INVALID_INPUT, "알 수 없는 리뷰 태그입니다: $code")
                    }.toSet(),
        )
}
