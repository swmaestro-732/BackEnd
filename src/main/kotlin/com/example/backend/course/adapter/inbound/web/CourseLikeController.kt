package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.CourseLikeResponse
import com.example.backend.course.application.port.inbound.CourseLikeUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 인바운드 어댑터 — 코스 좋아요/취소. SavedCourseController 미러. */
@RequiresAppFeature(AppFeature.USER_COURSE)
@RestController
class CourseLikeController(
    private val courseLikeUseCase: CourseLikeUseCase,
) {
    @PostMapping("/api/v1/courses/{courseId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun like(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
    ): ApiResponse<CourseLikeResponse> {
        val likesCnt = courseLikeUseCase.like(userId, courseId)
        return ApiResponse.success(
            CourseLikeResponse(courseId = courseId, liked = true, likesCnt = likesCnt),
            "코스를 좋아요했습니다.",
        )
    }

    @DeleteMapping("/api/v1/courses/{courseId}/likes")
    @AccessTokenRequired
    fun unlike(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
    ): ApiResponse<CourseLikeResponse> {
        val likesCnt = courseLikeUseCase.unlike(userId, courseId)
        return ApiResponse.success(
            CourseLikeResponse(courseId = courseId, liked = false, likesCnt = likesCnt),
            "코스 좋아요를 취소했습니다.",
        )
    }
}
