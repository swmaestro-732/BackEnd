package com.example.backend.course.adapter.inbound.web.v2

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.CourseIdResponse
import com.example.backend.course.adapter.inbound.web.v2.request.CreateCourseV2Request
import com.example.backend.course.adapter.inbound.web.v2.request.EditCourseV2Request
import com.example.backend.course.application.port.inbound.CourseUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 생성·수정 v2. 작성자가 고른 카테고리 여러 개(`categories`)를 받는다.
 * v1(`/api/v1/courses`)은 구버전 앱용으로 남겨 두며, 카테고리를 담은 장소들로 도출한다.
 */
@RequiresAppFeature(AppFeature.COURSE_CREATE)
@RestController
@RequestMapping("/api/v2/courses")
class CourseV2Controller(
    private val courseUseCase: CourseUseCase,
) {
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseV2Request,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseIdResponse> {
        if (mock) return ApiResponse.success(CourseIdResponse.MOCK)
        return ApiResponse.success(CourseIdResponse.from(courseUseCase.create(request.toCommand(userId))))
    }

    @PatchMapping("/{courseId}")
    fun edit(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: EditCourseV2Request,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseIdResponse> {
        if (mock) return ApiResponse.success(CourseIdResponse.MOCK)
        return ApiResponse.success(CourseIdResponse.from(courseUseCase.edit(request.toCommand(userId, courseId))))
    }
}
