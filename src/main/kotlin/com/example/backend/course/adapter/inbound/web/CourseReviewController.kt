package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseReviewRequest
import com.example.backend.course.adapter.inbound.web.response.CreateCourseReviewResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 인바운드 어댑터 */
@RequiresAppFeature("course-review")
@RestController
@RequestMapping("/api/v1/courses/{courseId}/reviews")
class CourseReviewController {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable courseId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseReviewRequest,
    ): ApiResponse<CreateCourseReviewResponse> =
        ApiResponse.success(CreateCourseReviewResponse(reviewId = NEXT_REVIEW_ID), "리뷰가 등록되었습니다.")

    @DeleteMapping("/{reviewId}")
    fun delete(
        @PathVariable courseId: Long,
        @PathVariable reviewId: Long,
        @CurrentUserId userId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("리뷰가 삭제되었습니다.")

    private companion object {
        /** 생성 모킹 고정 id — 목 리뷰(1~6) 다음 번호. */
        const val NEXT_REVIEW_ID = 7L
    }
}
