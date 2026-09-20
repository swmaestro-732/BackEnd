package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseCommentRequest
import com.example.backend.course.adapter.inbound.web.request.EditCourseCommentRequest
import com.example.backend.course.adapter.inbound.web.response.CourseCommentIdResponse
import com.example.backend.course.adapter.inbound.web.response.CourseCommentListResponse
import com.example.backend.course.application.port.inbound.CourseCommentQueryUseCase
import com.example.backend.course.application.port.inbound.CourseCommentUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/courses/{courseId}/comments")
class CourseCommentController(
    private val courseCommentUseCase: CourseCommentUseCase,
    private val courseCommentQueryUseCase: CourseCommentQueryUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun create(
        @PathVariable courseId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseCommentRequest,
    ): ApiResponse<CourseCommentIdResponse> {
        val result = courseCommentUseCase.create(request.toCommand(userId, courseId))
        return ApiResponse.success(CourseCommentIdResponse(result.id))
    }

    @GetMapping
    fun list(
        @PathVariable courseId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int = 20,
    ): ApiResponse<CourseCommentListResponse> =
        ApiResponse.success(
            CourseCommentListResponse.from(courseCommentQueryUseCase.list(courseId, viewerId, cursor, size)),
        )

    @PatchMapping("/{commentId}")
    @AccessTokenRequired
    fun edit(
        @PathVariable courseId: Long,
        @PathVariable commentId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: EditCourseCommentRequest,
    ): ApiResponse<Nothing?> {
        courseCommentUseCase.edit(request.toCommand(userId, courseId, commentId))
        return ApiResponse.ok()
    }

    @DeleteMapping("/{commentId}")
    @AccessTokenRequired
    fun delete(
        @PathVariable courseId: Long,
        @PathVariable commentId: Long,
        @CurrentUserId userId: Long,
    ): ApiResponse<Nothing?> {
        courseCommentUseCase.delete(userId, courseId, commentId)
        return ApiResponse.ok("댓글이 삭제되었습니다.")
    }
}
