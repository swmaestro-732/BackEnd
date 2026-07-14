package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse
import com.example.backend.course.application.port.inbound.CourseUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — HTTP 요청을 인바운드 포트([CourseUseCase]) 호출로 변환한다.
 * Result → Response 로 매핑해 도메인/애플리케이션 타입을 밖으로 노출하지 않는다.
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4041`).
 */
@RestController
@RequestMapping("/api/v1")
class CourseController(
    private val courseUseCase: CourseUseCase,
) {
    @GetMapping("/courses/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseDetailResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(CourseDetailResponse.from(courseUseCase.getDetail(courseId)))
    }
}
