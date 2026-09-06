package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseReviewRequest
import com.example.backend.course.adapter.inbound.web.response.CreateCourseReviewResponse
import com.example.backend.course.application.port.inbound.CourseReviewUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 인바운드 어댑터 — 코스 리뷰 작성·삭제 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/reviews")
class CourseReviewController(
    private val courseReviewUseCase: CourseReviewUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun create(
        @PathVariable courseId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseReviewRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CreateCourseReviewResponse> {
        if (mock && mockGuard.isMockAllowed()) {
            return ApiResponse.success(CreateCourseReviewResponse.MOCK, "리뷰가 등록되었습니다.")
        }

        val review = courseReviewUseCase.create(request.toCommand(courseId, userId))
        return ApiResponse.success(CreateCourseReviewResponse.from(review), "리뷰가 등록되었습니다.")
    }

    @DeleteMapping("/{reviewId}")
    @AccessTokenRequired
    fun delete(
        @PathVariable courseId: Long,
        @PathVariable reviewId: Long,
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("리뷰가 삭제되었습니다.")

        courseReviewUseCase.delete(userId = userId, courseId = courseId, reviewId = reviewId)
        return ApiResponse.ok("리뷰가 삭제되었습니다.")
    }
}
